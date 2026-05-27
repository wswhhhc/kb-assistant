package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/knowledge-bases/{kbId}/documents")
    public Result<?> upload(@PathVariable Long kbId,
                            @RequestParam("file") MultipartFile file,
                            @AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(documentService.upload(kbId, file, userDetails.getUserId()));
    }

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public Result<?> list(@PathVariable Long kbId,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(documentService.pageByKnowledgeBaseId(kbId, pageNum, pageSize));
    }

    @DeleteMapping("/documents/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }
}
