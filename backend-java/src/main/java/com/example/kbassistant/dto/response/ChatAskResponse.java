package com.example.kbassistant.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ChatAskResponse {
    private String answer;
    private List<Citation> citations;
    private Integer retrievalCount;
    private String modelName;
    private boolean success;

    @Data
    public static class Citation {
        private String fileName;
        private Integer pageNo;
        private Integer chunkIndex;
        private String contentPreview;
    }
}
