package com.example.kbassistant.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AiServiceClient {

    private final ObjectMapper objectMapper;

    @Value("${ai.service.document.process-url}")
    private String documentProcessUrl;

    @Value("${ai.service.document.timeout}")
    private long documentTimeout;

    @Value("${ai.service.chat.ask-url}")
    private String chatAskUrl;

    @Value("${ai.service.chat.stream-url}")
    private String chatStreamUrl;

    @Value("${ai.service.chat.timeout}")
    private long chatTimeout;

    @Value("${ai.service.chat.stream-timeout:${ai.service.chat.timeout}}")
    private long chatStreamTimeout;

    public AiServiceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map processDocument(Map<String, Object> request) {
        return postJsonForMap(documentProcessUrl, documentTimeout, request, "文档处理失败: ");
    }

    public Map askQuestion(Map<String, Object> request) {
        return postJsonForMap(chatAskUrl, chatTimeout, request, "问答请求失败: ");
    }

    public BufferedReader askQuestionStream(Map<String, Object> request) {
        return postJsonForStream(chatStreamUrl, chatStreamTimeout, request, "流式问答请求失败: ");
    }

    private Map postJsonForMap(String url, long readTimeout, Object request, String errorPrefix) {
        try {
            HttpURLConnection conn = openPostConnection(url, (int) readTimeout, "application/json");
            writeBody(conn, request);
            int status = conn.getResponseCode();
            if (status == 200) {
                return objectMapper.readValue(conn.getInputStream(), Map.class);
            }
            String error = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("AI 服务返回 " + status + ": " + error);
        } catch (Exception e) {
            throw new RuntimeException(errorPrefix + e.getMessage(), e);
        }
    }

    private BufferedReader postJsonForStream(String url, long readTimeout, Object request, String errorPrefix) {
        try {
            HttpURLConnection conn = openPostConnection(url, (int) readTimeout, "text/event-stream");
            writeBody(conn, request);
            int status = conn.getResponseCode();
            if (status == 200) {
                return new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            }
            String error = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("AI 服务返回 " + status + ": " + error);
        } catch (Exception e) {
            throw new RuntimeException(errorPrefix + e.getMessage(), e);
        }
    }

    private HttpURLConnection openPostConnection(String url, int readTimeout, String accept) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection(Proxy.NO_PROXY);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", accept);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(readTimeout);
        conn.setDoOutput(true);
        conn.connect();
        return conn;
    }

    private void writeBody(HttpURLConnection conn, Object request) throws Exception {
        byte[] json = objectMapper.writeValueAsBytes(request);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json);
            os.flush();
        }
    }
}
