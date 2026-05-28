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
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            HttpURLConnection conn = (HttpURLConnection) URI.create(documentProcessUrl).toURL().openConnection(Proxy.NO_PROXY);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout((int) documentTimeout);
            conn.setDoOutput(true);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json);
                os.flush();
            }
            int status = conn.getResponseCode();
            if (status == 200) {
                return objectMapper.readValue(conn.getInputStream(), Map.class);
            }
            String error = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("AI 服务返回 " + status + ": " + error);
        } catch (Exception e) {
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    public Map askQuestion(Map<String, Object> request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            HttpURLConnection conn = (HttpURLConnection) URI.create(chatAskUrl).toURL().openConnection(Proxy.NO_PROXY);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout((int) chatTimeout);
            conn.setDoOutput(true);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json);
                os.flush();
            }
            int status = conn.getResponseCode();
            if (status == 200) {
                return objectMapper.readValue(conn.getInputStream(), Map.class);
            }
            String error = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("AI 服务返回 " + status + ": " + error);
        } catch (Exception e) {
            throw new RuntimeException("问答请求失败: " + e.getMessage(), e);
        }
    }

    public BufferedReader askQuestionStream(Map<String, Object> request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            HttpURLConnection conn = (HttpURLConnection) URI.create(chatStreamUrl).toURL().openConnection(Proxy.NO_PROXY);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout((int) chatStreamTimeout);
            conn.setDoOutput(true);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json);
                os.flush();
            }
            int status = conn.getResponseCode();
            if (status == 200) {
                return new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            }
            String error = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("AI 服务返回 " + status + ": " + error);
        } catch (Exception e) {
            throw new RuntimeException("流式问答请求失败: " + e.getMessage(), e);
        }
    }
}
