package com.example.kbassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessResponse {
    private boolean success;
    private int chunkCount;
    private String modelName;
    private List<Map<String, Object>> chunks;
}
