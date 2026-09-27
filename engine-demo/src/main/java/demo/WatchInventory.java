package demo;

import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;

/**
 * Runs Debezium's MySQL connector inside this process, with no Kafka or Kafka Connect.
 * Each change to the inventory database is handed to a callback instead of a Kafka topic.
 */
public class WatchInventory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();

        // Mostly the same settings as register-mysql.json
        props.setProperty("name", "embedded-inventory");
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        props.setProperty("database.hostname", "localhost");
        props.setProperty("database.port", "3306");
        props.setProperty("database.user", "debezium");
        props.setProperty("database.password", "dbz");
        props.setProperty("database.server.id", "85744"); // must differ from the Connect connector's 184054
        props.setProperty("topic.prefix", "embedded");
        props.setProperty("database.include.list", "inventory");

        // Jobs Kafka Connect normally does, done here in memory: remember the binlog
        // position and the table schemas. Nothing survives a restart, so every run
        // starts with a fresh snapshot.
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");
        props.setProperty("schema.history.internal", "io.debezium.relational.history.MemorySchemaHistory");

        // Leave out the verbose "schema" block from each event
        props.setProperty("converter.schemas.enable", "false");

        DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying(WatchInventory::handle)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                engine.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }));

        System.out.println("Watching inventory. Snapshot first, then live changes. Ctrl-C to stop.");
        engine.run(); // blocks until closed
    }

    private static void handle(ChangeEvent<String, String> event) {
        if (event.value() == null) {
            System.out.printf("%-28s tombstone  key=%s%n", event.destination(), event.key());
            return;
        }
        try {
            JsonNode value = MAPPER.readTree(event.value());
            if (!value.has("op")) {
                return; // schema change event, not a row change
            }
            System.out.printf("%-28s op=%s  before=%s  after=%s%n",
                    event.destination(), value.get("op").asText(), value.get("before"), value.get("after"));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
