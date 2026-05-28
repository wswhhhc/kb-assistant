package com.example.kbassistant.service.impl;

import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.enums.DocumentStatus;
import com.example.kbassistant.service.DocumentProcessingService;
import com.example.kbassistant.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private final DocumentService documentService;

    public DocumentProcessingServiceImpl(@Lazy DocumentService documentService) {
        this.documentService = documentService;
    }

    @Async("documentTaskExecutor")
    @Override
    public void processAsync(Long documentId, Long userId, boolean isAdmin) {
        log.info("异步处理文档: {} (thread: {})", documentId, Thread.currentThread().getName());
        try {
            documentService.processDocument(documentId, userId, isAdmin);
        } catch (BusinessException e) {
            log.warn("文档处理失败(业务异常): {} - {}", documentId, e.getMessage());
        } catch (Exception e) {
            log.warn("文档处理失败(系统异常): {} - {}", documentId, e.getMessage());
        }
    }
}
