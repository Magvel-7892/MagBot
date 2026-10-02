package com.magbot.localagent;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Only talks to the fixed loopback endpoint. No remote URL or redirect is accepted. */
final class LlmClient {
    private static final String BASE = "http://127.0.0.1:8080";
    private final String apiKey;
    private volatile HttpURLConnection active;

    LlmClient(String apiKey) {
        this.apiKey = apiKey.trim();
        if (!this.apiKey.matches("[A-Za-z0-9_-]{16,128}")) {
            throw new IllegalArgumentException("Paste the local API key printed by start-magbot.sh.");
        }
    }

    void checkConnection() throws Exception {
        JSONObject body = new JSONObject(request("/v1/models", null));
        if (body.optJSONArray("data") == null) {
            throw new IllegalStateException("Unexpected server response. Check the local server.");
        }
    }

    String complete(String command) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "magbot-local");
        body.put("temperature", 0.0);
        body.put("max_tokens", 512);
        body.put("stream", false);
        body.put("response_format", new JSONObject().put("type", "json_object"));
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", AgentProtocol.systemPrompt()));
        messages.put(new JSONObject().put("role", "user").put("content", command));
        body.put("messages", messages);
        JSONObject json = new JSONObject(request("/v1/chat/completions", body.toString()));
        JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
        if (!"stop".equals(choice.optString("finish_reason"))) {
            throw new IllegalStateException("Model output was incomplete. Use a shorter, simpler command.");
        }
        Object content = choice.getJSONObject("message").opt("content");
        if (!(content instanceof String) || ((String) content).trim().isEmpty()) {
            throw new IllegalStateException("No final JSON answer. Check reasoning-off server settings.");
        }
        return (String) content;
    }

    private String request(String path, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE + path).openConnection();
        active = connection;
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(body == null ? 15000 : 180000);
            connection.setRequestMethod(body == null ? "GET" : "POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (body != null) {
                connection.setDoOutput(true);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
            }
            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = readLimited(input);
            if (status == 401 || status == 403) throw new IllegalStateException("The local API key is incorrect.");
            if (status == 503) throw new IllegalStateException("The model is still loading or busy. Try again shortly.");
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Local server HTTP " + status + ": "
                        + response.substring(0, Math.min(400, response.length())));
            }
            return response;
        } finally {
            connection.disconnect();
            if (active == connection) active = null;
        }
    }

    void cancel() {
        HttpURLConnection connection = active;
        if (connection != null) connection.disconnect();
    }

    private static String readLimited(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (bytes.size() + count > 65536) throw new IllegalStateException("Server response exceeds size limit.");
                bytes.write(buffer, 0, count);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
