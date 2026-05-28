package com.example.kbassistant.dto.response;

import com.example.kbassistant.entity.Document;
import com.example.kbassistant.entity.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentDetailVO {
    private Document document;
    private List<DocumentChunk> chunks;
}
