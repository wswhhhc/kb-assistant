package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    Document upload(Long knowledgeBaseId, MultipartFile file, Long userId);
    IPage<Document> pageByKnowledgeBaseId(Long knowledgeBaseId, int pageNum, int pageSize);
    Document getById(Long id);
    void delete(Long id);
    void processDocument(Long documentId);
}
