package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.KnowledgeBaseCreateRequest;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(knowledgeBaseService.page(pageNum, pageSize, userDetails.getUserId(), isAdmin));
    }

    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id,
                         @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(knowledgeBaseService.getById(id, userDetails.getUserId(), isAdmin));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody KnowledgeBaseCreateRequest request,
                            @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        return Result.success(knowledgeBaseService.create(request, userDetails.getUserId(), isAdmin));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody KnowledgeBaseCreateRequest request,
                               @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        knowledgeBaseService.update(id, request, userDetails.getUserId(), isAdmin);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = userDetails.isAdmin();
        knowledgeBaseService.delete(id, userDetails.getUserId(), isAdmin);
        return Result.success();
    }
}
