package com.example.kbassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ChatAskContext {
    private Long sessionId;
    private List<Map<String, String>> history;
}
