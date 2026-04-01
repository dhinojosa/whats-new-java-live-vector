package com.evolutionnext.simpleweb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArtistServer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * In-memory database:
     * key = slug
     * value = payload map
     */
    private final Map<String, Map<String, Object>> artists = new LinkedHashMap<>();

    public ArtistServer() {
        seedArtists();
    }

    private void seedArtists() {
        artists.put("ozzy-osbourne", artistPayload(
            "Ozzy",
            "Osbourne",
            1948,
            "Birmingham, England, United Kingdom"
        ));

        artists.put("sam-cooke", artistPayload(
            "Sam",
            "Cooke",
            1931,
            "Clarksdale, Mississippi, United States"
        ));

        artists.put("lemmy", artistPayload(
            "Ian",
            "Kilmister",
            1945,
            "Burslem, Stoke-on-Trent, England, United Kingdom"
        ));

        artists.put("taylor-swift", artistPayload(
            "Taylor",
            "Swift",
            1989,
            "West Reading, Pennsylvania, United States"
        ));

        artists.put("bob-marley", artistPayload(
            "Bob",
            "Marley",
            1945,
            "Nine Mile, Saint Ann Parish, Jamaica"
        ));
    }

    private Map<String, Object> artistPayload(
        String firstName,
        String lastName,
        int birthYear,
        String birthPlace
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("birthYear", birthYear);
        payload.put("birthPlace", birthPlace);
        return payload;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/artists", new ArtistsHandler());
        server.createContext("/artists/", new ArtistBySlugHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server started on http://localhost:8080");
    }

    /**
     * Handles:
     * GET /artists
     * POST /artists
     */
    private class ArtistsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                handleListArtists(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreateArtist(exchange);
            } else {
                sendText(exchange, 405, "Method Not Allowed");
            }
        }
    }

    /**
     * Handles:
     * GET /artists/{slug}
     */
    private class ArtistBySlugHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (!"GET".equalsIgnoreCase(method)) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String slug = path.substring("/artists/".length()).trim();

            if (slug.isEmpty()) {
                sendText(exchange, 400, "Artist slug is required");
                return;
            }

            Map<String, Object> artist = artists.get(slug);

            if (artist == null) {
                sendText(exchange, 404, "Artist not found");
                return;
            }

            sendJson(exchange, 200, artist);
        }
    }

    private void handleListArtists(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, artists);
    }

    private void handleCreateArtist(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            Map<String, Object> requestBody = objectMapper.readValue(
                inputStream,
                new TypeReference<Map<String, Object>>() {}
            );

            String slug = asString(requestBody.get("slug"));
            String firstName = asString(requestBody.get("firstName"));
            String lastName = asString(requestBody.get("lastName"));
            Integer birthYear = asInteger(requestBody.get("birthYear"));
            String birthPlace = asString(requestBody.get("birthPlace"));

            if (isBlank(slug) || isBlank(firstName) || isBlank(lastName)
                || birthYear == null || isBlank(birthPlace)) {
                sendText(exchange, 400,
                    "Required fields: slug, firstName, lastName, birthYear, birthPlace");
                return;
            }

            if (artists.containsKey(slug)) {
                sendText(exchange, 409, "Artist with slug already exists");
                return;
            }

            Map<String, Object> payload = artistPayload(firstName, lastName, birthYear, birthPlace);
            artists.put(slug, payload);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Artist created");
            response.put("slug", slug);
            response.put("artist", payload);

            sendJson(exchange, 201, response);
        } catch (Exception e) {
            sendText(exchange, 400, "Invalid JSON payload");
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(body);

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");

        exchange.sendResponseHeaders(statusCode, jsonBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(jsonBytes);
        }
    }

    private void sendText(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/plain; charset=utf-8");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static void main(String[] args) throws IOException {
        new ArtistServer().start();
    }
}
