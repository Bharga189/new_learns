package io.buffy.cdc.connect;

import com.fasterxml.jackson.databind.JsonNode;
import io.buffy.cdc.common.CdcConfig;
import io.buffy.cdc.common.JsonSupport;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * INTERMEDIATE: the Kafka Connect REST API is the control plane for Debezium.
 *
 * This CLI is deliberately small. It shows the three requests you will use
 * again and again:
 *
 *   POST   /connectors                    create
 *   PUT    /connectors/{name}/config      update an existing connector
 *   GET    /connectors/{name}/status      inspect state and tasks
 *
 * Run from learn/debezium-mysql-parent:
 *   mvn -q -pl 02-connect-deploy exec:java \
 *     -Dexec.args="register ../02-connect-deploy/src/main/resources/connectors/orders.json"
 *
 * The setup script wraps these commands for you.
 */
public final class ConnectAdmin {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private ConnectAdmin() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }

        switch (args[0]) {
            case "register" -> {
                requireArgs(args, 2);
                register(Path.of(args[1]));
            }
            case "status" -> {
                requireArgs(args, 2);
                print("status " + args[1], send("GET", "/connectors/" + encode(args[1]) + "/status", null));
            }
            case "delete" -> {
                requireArgs(args, 2);
                print("delete " + args[1], send("DELETE", "/connectors/" + encode(args[1]), null));
            }
            case "list" -> print("connectors", send("GET", "/connectors", null));
            default -> usage();
        }
    }

    private static void register(Path configFile) throws Exception {
        String body = Files.readString(configFile);
        JsonNode root = JsonSupport.parse(body);
        String name = root.path("name").asText("");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Connector config must contain a non-empty \"name\": " + configFile);
        }
        JsonNode config = root.path("config");
        if (config.isMissingNode() || !config.isObject()) {
            throw new IllegalArgumentException("Connector config must contain a \"config\" object: " + configFile);
        }

        HttpResponse<String> response;
        if (exists(name)) {
            System.out.println("Connector '" + name + "' already exists; updating its config.");
            response = send("PUT", "/connectors/" + encode(name) + "/config", config.toString());
        } else {
            System.out.println("Creating connector '" + name + "'.");
            response = send("POST", "/connectors", body);
        }

        print("register " + name, response);
        print("status " + name, send("GET", "/connectors/" + encode(name) + "/status", null));
    }

    private static boolean exists(String name) throws Exception {
        HttpResponse<String> response = send("GET", "/connectors/" + encode(name), null);
        if (response.statusCode() == 200) {
            return true;
        }
        if (response.statusCode() == 404) {
            return false;
        }
        throw new IllegalStateException("Connect returned HTTP " + response.statusCode()
                + " while checking connector '" + name + "': " + response.body());
    }

    private static HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(CdcConfig.connectUrl() + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body));
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void print(String label, HttpResponse<String> response) {
        System.out.println();
        System.out.println("--- " + label + " (HTTP " + response.statusCode() + ") ---");
        System.out.println(JsonSupport.pretty(response.body()));
        if (response.statusCode() >= 300) {
            throw new IllegalStateException(label + " failed with HTTP " + response.statusCode());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void requireArgs(String[] args, int count) {
        if (args.length < count) {
            usage();
            System.exit(2);
        }
    }

    private static void usage() {
        System.out.println("""
                Usage: ConnectAdmin <command> [argument]

                  list                              list deployed connectors
                  status <connector-name>           show connector + task state
                  delete <connector-name>           remove a connector
                  register <path/to/connector.json> create or update a connector

                The Connect REST endpoint defaults to http://localhost:8083 and can
                be overridden with CONNECT_URL.
                """);
    }
}
