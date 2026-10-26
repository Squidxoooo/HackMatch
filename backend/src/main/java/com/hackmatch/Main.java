package com.hackmatch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Starts the small Core Java HTTP API for HackMatch. */
public class Main {
    private static final String FRONTEND_ORIGIN = "http://localhost:5500";
    private static final int MAX_REQUEST_BYTES = 100_000;
    private final HackathonDAO hackathonDAO = new HackathonDAO();
    private final BookmarkDAO bookmarkDAO = new BookmarkDAO();
    private final MatchingService matchingService = new MatchingService();

    public static void main(String[] args) throws IOException {
        int port = getPort();
        Main application = new Main();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/health", application::handleHealth);
        server.createContext("/api/hackathons", application::handleHackathons);
        server.createContext("/api/recommendations", application::handleRecommendations);
        server.createContext("/api/bookmarks", application::handleBookmarks);
        server.setExecutor(null); // Use the small built-in executor for this mini-project.
        server.start();

        System.out.println("HackMatch API started at http://localhost:" + port + "/api");
        System.out.println("CORS allows the frontend at " + FRONTEND_ORIGIN);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange) || rejectUnexpectedMethod(exchange, "GET")) return;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "running");
        try {
            hackathonDAO.canConnect();
            response.put("database", "connected");
        } catch (SQLException exception) {
            // The server is still running, but this tells the student exactly why data endpoints cannot work yet.
            response.put("database", "unavailable");
            response.put("databaseMessage", exception.getMessage());
        }
        sendJson(exchange, 200, response);
    }

    private void handleHackathons(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange) || rejectUnexpectedMethod(exchange, "GET")) return;

        try {
            String suffix = pathSuffix(exchange, "/api/hackathons");
            if (suffix.isEmpty() || suffix.equals("/")) {
                List<Map<String, Object>> response = new ArrayList<>();
                for (Hackathon hackathon : hackathonDAO.getAllHackathons()) {
                    response.add(hackathon.toApiMap());
                }
                sendJson(exchange, 200, response);
                return;
            }

            int id = parsePathId(suffix);
            Hackathon hackathon = hackathonDAO.getHackathonById(id);
            if (hackathon == null) {
                sendError(exchange, 404, "Hackathon not found.");
                return;
            }
            sendJson(exchange, 200, hackathon.toApiMap());
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());
        } catch (SQLException exception) {
            sendDatabaseError(exchange, exception);
        }
    }

    private void handleRecommendations(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange) || rejectUnexpectedMethod(exchange, "POST")) return;

        try {
            StudentProfile student = parseStudentProfile(readRequestBody(exchange));
            List<MatchResult> results = matchingService.getRecommendations(student, hackathonDAO.getAllHackathons());
            List<Map<String, Object>> response = new ArrayList<>();
            for (MatchResult result : results) {
                Map<String, Object> recommendation = new LinkedHashMap<>();
                // These three names exactly match the existing frontend prototype.
                recommendation.put("hackathonId", result.getHackathon().getId());
                recommendation.put("score", result.getScore());
                recommendation.put("reasons", result.getReasons());
                response.add(recommendation);
            }
            sendJson(exchange, 200, response);
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());
        } catch (SQLException exception) {
            sendDatabaseError(exchange, exception);
        }
    }

    private void handleBookmarks(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) return;

        try {
            if (exchange.getRequestMethod().equals("GET")) {
                int studentId = parsePositiveInt(getQueryParameters(exchange.getRequestURI()).get("studentId"), "studentId");
                if (!bookmarkDAO.studentExists(studentId)) {
                    sendError(exchange, 404, "Student profile not found.");
                    return;
                }
                List<Map<String, Object>> response = new ArrayList<>();
                for (Bookmark bookmark : bookmarkDAO.getBookmarksByStudentId(studentId)) {
                    response.add(bookmarkToMap(bookmark));
                }
                sendJson(exchange, 200, response);
                return;
            }

            if (exchange.getRequestMethod().equals("POST")) {
                Map<String, Object> request = JsonUtil.parseObject(readRequestBody(exchange));
                int studentId = getRequiredPositiveInt(request, "studentId");
                int hackathonId = getRequiredPositiveInt(request, "hackathonId");
                if (!bookmarkDAO.studentExists(studentId)) {
                    sendError(exchange, 404, "Student profile not found.");
                    return;
                }
                Hackathon hackathon = hackathonDAO.getHackathonById(hackathonId);
                if (hackathon == null) {
                    sendError(exchange, 404, "Hackathon not found.");
                    return;
                }

                int bookmarkId = bookmarkDAO.createBookmark(studentId, hackathonId);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("id", bookmarkId);
                response.put("studentId", studentId);
                response.put("hackathon", hackathon.toApiMap());
                sendJson(exchange, 201, response);
                return;
            }

            if (exchange.getRequestMethod().equals("DELETE")) {
                int bookmarkId = parsePathId(pathSuffix(exchange, "/api/bookmarks"));
                if (!bookmarkDAO.deleteBookmark(bookmarkId)) {
                    sendError(exchange, 404, "Bookmark not found.");
                    return;
                }
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Bookmark removed.");
                sendJson(exchange, 200, response);
                return;
            }

            sendMethodNotAllowed(exchange, "GET, POST, DELETE, OPTIONS");
        } catch (SQLIntegrityConstraintViolationException exception) {
            // The table's unique constraint rejects duplicate bookmarks.
            sendError(exchange, 409, "This hackathon is already bookmarked by this student.");
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());
        } catch (SQLException exception) {
            sendDatabaseError(exchange, exception);
        }
    }

    private StudentProfile parseStudentProfile(String requestBody) {
        Map<String, Object> request = JsonUtil.parseObject(requestBody);
        List<String> skills = getRequiredStringList(request, "skills");
        List<String> interests = getRequiredStringList(request, "interests");
        String experienceLevel = getRequiredString(request, "experienceLevel");
        if (!List.of("Beginner", "Intermediate", "Advanced").contains(experienceLevel)) {
            throw new IllegalArgumentException("experienceLevel must be Beginner, Intermediate, or Advanced.");
        }

        double minimumPrize = getOptionalNonNegativeNumber(request, "minPrize", 0);
        int teamSize = getRequiredPositiveInt(request, "teamSize");
        if (teamSize > 10) {
            throw new IllegalArgumentException("teamSize must be between 1 and 10.");
        }
        String name = getOptionalString(request, "name", "Anonymous Student");
        return new StudentProfile(name, skills, interests, experienceLevel, minimumPrize, teamSize);
    }

    private Map<String, Object> bookmarkToMap(Bookmark bookmark) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", bookmark.getId());
        result.put("studentId", bookmark.getStudentId());
        result.put("createdAt", bookmark.getCreatedAt() == null ? null
                : bookmark.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.put("hackathon", bookmark.getHackathon().toApiMap());
        return result;
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("OPTIONS")) return false;
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return true;
    }

    private boolean rejectUnexpectedMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        if (exchange.getRequestMethod().equals(expectedMethod)) return false;
        sendMethodNotAllowed(exchange, expectedMethod + ", OPTIONS");
        return true;
    }

    private void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendError(exchange, 405, "Method not allowed.");
    }

    private void sendDatabaseError(HttpExchange exchange, SQLException exception) throws IOException {
        System.err.println("Database error: " + exception.getMessage());
        sendError(exchange, 500, "Database request failed. Check the API terminal and database configuration.");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null && Long.parseLong(contentLength) > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("Request body is too large.");
        }
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("Request body is too large.");
        }
        String body = new String(bytes, StandardCharsets.UTF_8).trim();
        if (body.isEmpty()) throw new IllegalArgumentException("Request body cannot be empty.");
        return body;
    }

    private int parsePathId(String suffix) {
        if (suffix == null || !suffix.matches("/[1-9][0-9]*")) {
            throw new IllegalArgumentException("URL must end with a positive numeric id.");
        }
        return parsePositiveInt(suffix.substring(1), "id");
    }

    private String pathSuffix(HttpExchange exchange, String basePath) {
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(basePath)) throw new IllegalArgumentException("Invalid API path.");
        return path.substring(basePath.length());
    }

    private int getRequiredPositiveInt(Map<String, Object> request, String name) {
        return parsePositiveInt(request.get(name), name);
    }

    private int parsePositiveInt(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required.");
        try {
            int number;
            if (value instanceof Number) {
                double decimal = ((Number) value).doubleValue();
                if (decimal != Math.floor(decimal)) throw new NumberFormatException();
                number = (int) decimal;
            } else {
                number = Integer.parseInt(String.valueOf(value));
            }
            if (number < 1) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a positive whole number.");
        }
    }

    private double getOptionalNonNegativeNumber(Map<String, Object> request, String name, double fallback) {
        Object value = request.get(name);
        if (value == null) return fallback;
        try {
            double number = value instanceof Number ? ((Number) value).doubleValue()
                    : Double.parseDouble(String.valueOf(value));
            if (number < 0 || Double.isNaN(number) || Double.isInfinite(number)) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a non-negative number.");
        }
    }

    private String getRequiredString(Map<String, Object> request, String name) {
        Object value = request.get(name);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return ((String) value).trim();
    }

    private String getOptionalString(Map<String, Object> request, String name, String fallback) {
        Object value = request.get(name);
        return value instanceof String && !((String) value).isBlank() ? ((String) value).trim() : fallback;
    }

    private List<String> getRequiredStringList(Map<String, Object> request, String name) {
        Object value = request.get(name);
        if (!(value instanceof List)) throw new IllegalArgumentException(name + " must be a non-empty array of strings.");

        List<String> values = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (!(item instanceof String) || ((String) item).isBlank()) {
                throw new IllegalArgumentException(name + " must contain only non-empty strings.");
            }
            values.add(((String) item).trim());
        }
        if (values.isEmpty()) throw new IllegalArgumentException(name + " must contain at least one value.");
        return values;
    }

    private Map<String, String> getQueryParameters(URI uri) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) return parameters;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            parameters.put(key, value);
        }
        return parameters;
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", message);
        sendJson(exchange, status, error);
    }

    private void sendJson(HttpExchange exchange, int status, Object response) throws IOException {
        byte[] body = JsonUtil.toJson(response).getBytes(StandardCharsets.UTF_8);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", FRONTEND_ORIGIN);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static int getPort() {
        String portText = System.getenv("HM_PORT");
        if (portText == null || portText.isBlank()) return 8080;
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException exception) {
            System.err.println("Invalid HM_PORT. Using 8080 instead.");
            return 8080;
        }
    }
}
