package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.FailedQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/failed-questions")
@RequiredArgsConstructor
public class FailedQuestionController {

    private final FailedQuestionService failedQuestionService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) Long kbId,
                          @RequestParam(required = false) String failureType,
                          @RequestParam(required = false) String status) {
        return Result.success(failedQuestionService.page(pageNum, pageSize, kbId, failureType, status));
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.success(failedQuestionService.getStats());
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  @RequestParam String status,
                                  @RequestParam(required = false) String resolution,
                                  @AuthenticationPrincipal JwtUserDetails userDetails) {
        failedQuestionService.updateStatus(id, status, resolution, userDetails.getUserId());
        return Result.success(null);
    }
}
