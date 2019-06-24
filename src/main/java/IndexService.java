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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static spark.Spark.post;

public class IndexService {

    public static void main(String... var) throws IOException {

        // Kafka Boilerplate
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.48.244:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", org.apache.kafka.common.serialization.ByteBufferSerializer.class);
        props.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer.class);

        Producer<String, String> producer = new KafkaProducer<>(props);

        // Get the schema definition
        final Schema schema;
        try (InputStream inputStream = new URL("https://code.gerdi-project.de/projects/HA/repos/jsonschema/raw/schema.json?at=72d1b96336045c92dbda5075db0f3187b8549e23").openStream()) {
            if (inputStream == null) throw new IllegalStateException("Schema definition could not be loaded.");
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            schema = SchemaLoader.load(rawSchema);
        }

        post("/index", (req, res) -> {
            if (req.contentType() == null || !req.contentType().startsWith("application/x-ndjson")) {
                res.status(405);
                return "{\"message\": \"Only requests of the content-type 'application/x-ndjson' are accepted.\"}";
            }
            res.type("application/json");
            List<Object> errs = new ArrayList<>();
            int lineNumber = 1;
            int docsAck = 0;
            try (BufferedReader reader = req.raw().getReader()) {
                while(reader.ready()) {
                    String line = reader.readLine();
                    JSONObject json;
                    try {
                        json = new JSONObject(line);
                    } catch (JSONException e) {
                        errs.add(new ParseError(lineNumber++));
                        continue;
                    }
                    if (!json.has("_id")) {
                        errs.add(new ParseError(lineNumber++, "_id is missing."));
                        continue;
                    }
                    if (json.has("action")) {
                        String action = json.getString("action");
                        if (!(action.equals("index") || action.equals("delete"))){
                            errs.add(new ParseError(lineNumber++, "Only 'index' and 'delete' are allowed actions."));
                            continue;
                        }
                    } else {
                        errs.add(new ParseError(lineNumber++, "action is missing."));
                        continue;
                    }

                    if (json.getString("action").equals("index")) {
                        if (!json.has("doc")) {
                            errs.add(new ParseError(lineNumber++, "doc field is missing."));
                            continue;
                        } else {
                            final JSONObject doc = json.getJSONObject("doc");
                            try {
                                schema.validate(doc);
                            } catch (ValidationException e) {
                                errs.add(e.toJSON().put("lineNumber", lineNumber++));
                                continue;
                            }
                        }
                    }

                    producer.send(new ProducerRecord<>("index", json.toString()));
                    docsAck++;
                    lineNumber++;
                }
            }
            if (errs.size() > 0) {
                return "{\"message\": \"" + docsAck + " documents have been acknowledged.\", \"errors\": " + new JSONArray(errs).toString() + "}";
            }
            return "{\"message\": \"" + docsAck + " documents have been acknowledged.\"}";
        });
    }
}
