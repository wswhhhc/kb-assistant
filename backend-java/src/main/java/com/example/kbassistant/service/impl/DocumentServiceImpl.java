package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.entity.Document;
import com.example.kbassistant.mapper.DocumentMapper;
import com.example.kbassistant.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public Document upload(Long knowledgeBaseId, MultipartFile file, Long userId) {
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

        return doc;
    }

    @Override
    public IPage<Document> pageByKnowledgeBaseId(Long knowledgeBaseId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getCreatedAt);
        return documentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Document getById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        documentMapper.deleteById(id);
    }

    @Override
    public void processDocument(Long documentId) {
        // 将在第三阶段实现：调用 FastAPI 处理文档
    }

    private boolean isSupportedType(String ext) {
        return "PDF".equals(ext) || "DOCX".equals(ext) || "MD".equals(ext) || "TXT".equals(ext);
    }
}
