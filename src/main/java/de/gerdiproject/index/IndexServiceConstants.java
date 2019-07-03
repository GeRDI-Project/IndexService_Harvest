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

/**
 * This class provides constants to the index service.
 *
 * @author Nelson Tavares de Sousa
 */
public class IndexServiceConstants {

    // Config constants
    public static final String INDEX_PATH = System.getenv()
            .getOrDefault("GERDI_INDEX_INDEX_PATH", "/index");
    public static final String KAFKA_BOOTSTRAP_SERVERS = System.getenv()
            .getOrDefault("GERDI_INDEX_KAFKA_BOOTSTRAP_SERVERS", "kafka-0.kafka-service.default.svc.cluster.local:9092");
    public static final String SCHEMA_LOCATION = System.getenv()
            .getOrDefault("GERDI_INDEX_SCHEMA_LOCATION", "classpath:schema.json");
    public static final String KAFKA_TOPIC_NAME = System.getenv()
            .getOrDefault("GERDI_INDEX_KAFKA_TOPIC_NAME", "index");

    // String constants
    public static final String WRONG_CONTENTTYPE = "{\"message\": \"Only requests of the content-type 'application/x-ndjson' are accepted.\"}";
    public static final String EXEC_TIME = "It took %d ms to process %d documents.";

    // Action values
    public static final String INDEX_ACTION = "index";
    public static final String DELETE_ACTION = "delete";

    // Field names
    public static final String ACTION_FIELDNAME = "action";
    public static final String ID_FIELDNAME = "_id";
    public static final String DOC_FIELDNAME = "doc";

    // Error messages
    public static final String ERR_DOC_MISSING = "doc field is missing.";
    public static final String ERR_ACTION_MISSING = "action is missing.";
    public static final String ERR_INVALID_ACTION = "Only 'index' and 'delete' are allowed actions.";
    public static final String ERR_ID_MISSING = "_id is missing.";

    // Responses
    public static final String NORMAL_RESPONSE = "{\"message\": \"%d documents have been acknowledged.\"}";
    public static final String ERROR_RESPONSE = "{\"message\": \"%d documents have been acknowledged.\", \"errors\": %s}";
}
