package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DocumentService {
    Document upload(Long knowledgeBaseId, MultipartFile file, Long userId, boolean isAdmin);
    IPage<Document> pageByKnowledgeBaseId(Long knowledgeBaseId, int pageNum, int pageSize, Long userId, boolean isAdmin);
    Document getById(Long id);
    Map<String, Object> getDocumentDetail(Long documentId, Long userId, boolean isAdmin);
    void delete(Long id, Long userId, boolean isAdmin);
    Map<String, Object> processDocument(Long documentId, Long userId, boolean isAdmin);
}
