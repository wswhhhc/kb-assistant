package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.MemberAddRequest;
import com.example.kbassistant.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-bases/{kbId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public Result<?> list(@PathVariable Long kbId) {
        return Result.success(memberService.listByKnowledgeBaseId(kbId));
    }

    @PostMapping
    public Result<Void> add(@PathVariable Long kbId,
                            @Valid @RequestBody MemberAddRequest request) {
        memberService.addMember(kbId, request.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long kbId, @PathVariable Long userId) {
        memberService.removeMember(kbId, userId);
        return Result.success();
    }
}
