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
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(documentService.upload(kbId, file, userDetails.getUserId(), isAdmin));
    }

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public Result<?> list(@PathVariable Long kbId,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(documentService.pageByKnowledgeBaseId(kbId, pageNum, pageSize, userDetails.getUserId(), isAdmin));
    }

    @GetMapping("/documents/{id}")
    public Result<?> detail(@PathVariable Long id,
                            @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(documentService.getDocumentDetail(id, userDetails.getUserId(), isAdmin));
    }

    @DeleteMapping("/documents/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        documentService.delete(id, userDetails.getUserId(), isAdmin);
        return Result.success();
    }

    @PostMapping("/documents/{id}/process")
    public Result<?> process(@PathVariable Long id,
                             @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(documentService.processDocument(id, userDetails.getUserId(), isAdmin));
    }
}
