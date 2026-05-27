package com.example.kbassistant.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.document.process-url}")
    private String documentProcessUrl;

    @Value("${ai.service.document.timeout}")
    private long documentTimeout;

    @Value("${ai.service.chat.ask-url}")
    private String chatAskUrl;

    @Value("${ai.service.chat.timeout}")
    private long chatTimeout;

    public AiServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public Map processDocument(Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(documentProcessUrl, entity, Map.class);
    }

    public Map askQuestion(Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(chatAskUrl, entity, Map.class);
    }
}
