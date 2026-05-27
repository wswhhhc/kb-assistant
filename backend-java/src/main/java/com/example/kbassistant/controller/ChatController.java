package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.ChatAskRequest;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.ChatMessageService;
import com.example.kbassistant.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;

    @PostMapping("/sessions")
    public Result<?> createSession(@RequestParam Long knowledgeBaseId,
                                   @AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(sessionService.create(userDetails.getUserId(), knowledgeBaseId));
    }

    @GetMapping("/sessions")
    public Result<?> listSessions(@RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(sessionService.pageByUserId(userDetails.getUserId(), pageNum, pageSize));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        sessionService.delete(id);
        return Result.success();
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<?> getMessages(@PathVariable Long id,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "50") int pageSize) {
        return Result.success(messageService.pageBySessionId(id, pageNum, pageSize));
    }

    @PostMapping("/ask")
    public Result<?> ask(@Valid @RequestBody ChatAskRequest request,
                         @AuthenticationPrincipal JwtUserDetails userDetails) {
        // 将在第四阶段完整实现问答流程
        return Result.success();
    }
}
