package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.FeedbackRequest;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody FeedbackRequest request,
                               @AuthenticationPrincipal JwtUserDetails userDetails) {
        feedbackService.submit(request, userDetails.getUserId());
        return Result.success();
    }

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(feedbackService.page(pageNum, pageSize));
    }

    @GetMapping("/my")
    public Result<?> myFeedback(@RequestParam String messageIds,
                                @AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(feedbackService.getMyFeedbackMap(messageIds, userDetails.getUserId()));
    }
}
