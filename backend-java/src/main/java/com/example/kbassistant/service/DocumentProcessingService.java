package com.example.kbassistant.service;

public interface DocumentProcessingService {
    void processAsync(Long documentId, Long userId, boolean isAdmin);
}
