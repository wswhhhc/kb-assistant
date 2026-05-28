package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.client.AiServiceClient;
import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.entity.Document;
import com.example.kbassistant.entity.DocumentChunk;
import com.example.kbassistant.entity.KnowledgeBase;
import com.example.kbassistant.mapper.DocumentChunkMapper;
import com.example.kbassistant.mapper.DocumentMapper;
import com.example.kbassistant.service.DocumentService;
import com.example.kbassistant.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final AiServiceClient aiServiceClient;
    private final KnowledgeBaseService knowledgeBaseService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public Document upload(Long knowledgeBaseId, MultipartFile file, Long userId, boolean isAdmin) {
        // 检查写入权限：管理员或知识库拥有者
        KnowledgeBase kb = knowledgeBaseService.getById(knowledgeBaseId, userId, isAdmin);
        if (!isAdmin && !kb.getOwnerUserId().equals(userId)) {
            throw new BusinessException("无权限向该知识库上传文档");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toUpperCase();
        if (!isSupportedType(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }

        String uuidName = UUID.randomUUID() + "_" + originalName;
        String relativePath = knowledgeBaseId + "/" + uuidName;
        String fullPath = uploadDir + "/" + relativePath;

        try {
            Path targetPath = Paths.get(fullPath);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        Document doc = new Document();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setFileName(originalName);
        doc.setFileType(ext);
        doc.setFilePath(relativePath);
        doc.setFileSize(file.getSize());
        doc.setParseStatus("UPLOADED");
        doc.setCreatedBy(userId);
        documentMapper.insert(doc);

        // 上传成功后自动触发文档处理
        final Long docId = doc.getId();
        new Thread(() -> {
            try {
                log.info("自动处理文档: {}", docId);
                processDocument(docId, userId, isAdmin);
            } catch (Exception e) {
                log.warn("文档自动处理失败: {} - {}", docId, e.getMessage());
            }
        }).start();

        return doc;
    }

    @Override
    public IPage<Document> pageByKnowledgeBaseId(Long knowledgeBaseId, int pageNum, int pageSize, Long userId, boolean isAdmin) {
        // 检查读取权限
        knowledgeBaseService.getById(knowledgeBaseId, userId, isAdmin);

        return documentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(Document::getCreatedAt)
        );
    }

    @Override
    public Document getById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public Map<String, Object> getDocumentDetail(Long documentId, Long userId, boolean isAdmin) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        // 检查知识库读取权限
        knowledgeBaseService.getById(doc.getKnowledgeBaseId(), userId, isAdmin);

        var chunkWrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex);
        var chunks = documentChunkMapper.selectList(chunkWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("document", doc);
        result.put("chunks", chunks);
        return result;
    }

    @Override
    public void delete(Long id, Long userId, boolean isAdmin) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        // 校验写入权限
        KnowledgeBase kb = knowledgeBaseService.getById(doc.getKnowledgeBaseId(), userId, isAdmin);
        if (!isAdmin && !kb.getOwnerUserId().equals(userId)) {
            throw new BusinessException("无权限删除该文档");
        }

        documentMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> processDocument(Long documentId, Long userId, boolean isAdmin) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        // 校验写入权限
        KnowledgeBase kb = knowledgeBaseService.getById(doc.getKnowledgeBaseId(), userId, isAdmin);
        if (!isAdmin && !kb.getOwnerUserId().equals(userId)) {
            throw new BusinessException("无权限处理该文档");
        }

        if (!"UPLOADED".equals(doc.getParseStatus()) && !"FAILED".equals(doc.getParseStatus())) {
            throw new BusinessException("文档状态不允许处理: " + doc.getParseStatus());
        }

        // 失败重试时清空错误信息
        if ("FAILED".equals(doc.getParseStatus())) {
            doc.setErrorMessage(null);
            documentMapper.updateById(doc);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("documentId", doc.getId());
        request.put("knowledgeBaseId", doc.getKnowledgeBaseId());
        request.put("filePath", uploadDir + "/" + doc.getFilePath());
        request.put("fileName", doc.getFileName());
        request.put("fileType", doc.getFileType());

        try {
            return aiServiceClient.processDocument(request);
        } catch (Exception e) {
            doc.setParseStatus("FAILED");
            doc.setErrorMessage("调用 AI 服务失败: " + e.getMessage());
            documentMapper.updateById(doc);
            throw new BusinessException("文档处理失败: " + e.getMessage());
        }
    }

    private boolean isSupportedType(String ext) {
        return "PDF".equals(ext) || "DOCX".equals(ext) || "MD".equals(ext) || "TXT".equals(ext);
    }
}
