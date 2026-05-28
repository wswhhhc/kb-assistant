package com.example.kbassistant.dto.response;

import lombok.Data;

@Data
public class FailedQuestionStatsVO {
    private long totalCount;
    private long noHitCount;
    private long lowQualityCount;
    private long modelErrorCount;
    private long insufficientCitationCount;
    private long pendingCount;
}
