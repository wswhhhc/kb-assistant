package com.example.kbassistant.controller;

import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.MemberAddRequest;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                            @Valid @RequestBody MemberAddRequest request,
                            @AuthenticationPrincipal JwtUserDetails userDetails) {
        if (!"ADMIN".equals(userDetails.getRole())) {
            throw new BusinessException("无权限，仅管理员可管理成员");
        }
        memberService.addMember(kbId, request.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long kbId,
                               @PathVariable Long userId,
                               @AuthenticationPrincipal JwtUserDetails userDetails) {
        if (!"ADMIN".equals(userDetails.getRole())) {
            throw new BusinessException("无权限，仅管理员可管理成员");
        }
        memberService.removeMember(kbId, userId);
        return Result.success();
    }
}
