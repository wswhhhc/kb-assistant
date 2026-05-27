package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.service.FailedQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/failed-questions")
@RequiredArgsConstructor
public class FailedQuestionController {

    private final FailedQuestionService failedQuestionService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(failedQuestionService.page(pageNum, pageSize));
    }
}
