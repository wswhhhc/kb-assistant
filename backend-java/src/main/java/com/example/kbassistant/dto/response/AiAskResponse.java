package com.example.kbassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAskResponse {
    private String answer;
    private List<Map<String, Object>> citations;
    private int retrievalCount;
    private String modelName;
    private boolean success;
}
