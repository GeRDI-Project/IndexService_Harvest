/**
 * Copyright © 2019 Nelson Tavares de Sousa (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.index;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.ResourceUtils;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static spark.Spark.port;
import static spark.Spark.post;

/**
 * This class provides the logic for providing an interface to index documents in GeRDI. Running the main method initializes a server with a ReST endpoint on /index.
 *
 * @author Nelson Tavares de Sousa
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexService.class);
    private static InputStream inputStream;

    /**
     * Initializes and starts the server.
     * @param var Arguments provided by the environment. None are evaluated.
     * @throws IOException if schema can not be found.
     */
    public static final void main(final String... var) throws IOException {

        // Kafka Boilerplate
        final Properties props = new Properties();
        props.put("bootstrap.servers", IndexServiceConstants.KAFKA_BOOTSTRAP_SERVERS);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", org.apache.kafka.common.serialization.ByteBufferSerializer.class);
        props.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer.class);

        final Producer<String, String> producer = new KafkaProducer<>(props);

        // Get the schema definition and load it
        final Schema schema;
        inputStream = ResourceUtils.getURL(IndexServiceConstants.SCHEMA_LOCATION).openStream();
        final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
        schema = SchemaLoader.load(rawSchema);

        port(4567);

        post(IndexServiceConstants.INDEX_PATH, (req, res) -> {
            if (req.contentType() == null || !req.contentType().startsWith("application/x-ndjson")) {
                res.status(405);
                return IndexServiceConstants.WRONG_CONTENTTYPE;
            }
            res.type("application/json");
            final List<Object> errs = new ArrayList<>();
            int lineNumber = 1;
            int docsAck = 0;
            final long startTime = Instant.now().toEpochMilli();
            try (BufferedReader reader = req.raw().getReader()) {
                while(reader.ready()) {
                    final String line = reader.readLine();
                    final JSONObject json;
                    try {
                        json = new JSONObject(line);
                    } catch (JSONException e) {
                        errs.add(new ParseError(lineNumber++));
                        continue;
                    }
                    if (!json.has(IndexServiceConstants.ID_FIELDNAME)) {
                        errs.add(new ParseError(lineNumber++, IndexServiceConstants.ERR_ID_MISSING));
                        continue;
                    }
                    if (json.has(IndexServiceConstants.ACTION_FIELDNAME)) {
                        final String action = json.getString(IndexServiceConstants.ACTION_FIELDNAME);
                        if (!(action.equals(IndexServiceConstants.INDEX_ACTION) || action.equals(IndexServiceConstants.DELETE_ACTION))){
                            errs.add(new ParseError(lineNumber++, IndexServiceConstants.ERR_INVALID_ACTION));
                            continue;
                        }
                    } else {
                        errs.add(new ParseError(lineNumber++, IndexServiceConstants.ERR_ACTION_MISSING));
                        continue;
                    }

                    if (json.getString(IndexServiceConstants.ACTION_FIELDNAME).equals(IndexServiceConstants.INDEX_ACTION)) {
                        if (json.has(IndexServiceConstants.DOC_FIELDNAME)) {
                            final JSONObject doc = json.getJSONObject(IndexServiceConstants.DOC_FIELDNAME);
                            try {
                                schema.validate(doc);
                            } catch (ValidationException e) {
                                errs.add(e.toJSON().put("lineNumber", lineNumber++));
                                continue;
                            }
                        } else {
                            errs.add(new ParseError(lineNumber++, IndexServiceConstants.ERR_DOC_MISSING));
                            continue;
                        }
                    }

                    producer.send(new ProducerRecord<>(IndexServiceConstants.KAFKA_TOPIC_NAME, json.toString()));
                    docsAck++;
                    lineNumber++;
                }
            }

            final long endTime = Instant.now().toEpochMilli();
            final long duration = endTime - startTime;
            LOGGER.info(String.format(IndexServiceConstants.EXEC_TIME, duration, lineNumber));

            if (errs.size() > 0) {
                return String.format(IndexServiceConstants.ERROR_RESPONSE, docsAck, new JSONArray(errs).toString());
            }
            return String.format(IndexServiceConstants.NORMAL_RESPONSE, docsAck);
        });
    }
}
