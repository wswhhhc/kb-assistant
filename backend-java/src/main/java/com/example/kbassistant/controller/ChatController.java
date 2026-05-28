package com.example.kbassistant.controller;

import com.example.kbassistant.client.AiServiceClient;
import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.ChatAskRequest;
import com.example.kbassistant.entity.ChatMessage;
import com.example.kbassistant.entity.ChatSession;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.ChatMessageService;
import com.example.kbassistant.service.ChatSessionService;
import com.example.kbassistant.service.FailedQuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final FailedQuestionService failedQuestionService;

    @PostMapping("/sessions")
    public Result<?> createSession(@RequestParam Long knowledgeBaseId,
                                   @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean isAdmin = "ADMIN".equals(userDetails.getRole());
        return Result.success(sessionService.create(userDetails.getUserId(), knowledgeBaseId, isAdmin));
    }

    @GetMapping("/sessions")
    public Result<?> listSessions(@RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) Long knowledgeBaseId,
                                  @AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(sessionService.pageByUserId(userDetails.getUserId(), knowledgeBaseId, pageNum, pageSize));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        sessionService.delete(id);
        return Result.success();
    }

    @GetMapping("/sessions/{id}")
    public Result<?> getSession(@PathVariable Long id) {
        return Result.success(sessionService.getById(id));
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
        Long userId = userDetails.getUserId();
        boolean isAdmin = "ADMIN".equals(userDetails.getRole());

        Long sessionId = request.getSessionId();
        if (sessionId == null) {
            ChatSession session = sessionService.create(userId, request.getKnowledgeBaseId(), isAdmin);
            sessionId = session.getId();
        }
        final Long finalSessionId = sessionId;

        // 加载历史消息（多轮上下文，按时间正序）
        List<ChatMessage> recentMessages = messageService.findRecentBySessionId(finalSessionId, 50);
        List<Map<String, String>> history = new ArrayList<>();
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            Map<String, String> entry = new HashMap<>();
            entry.put("role", "AI".equals(msg.getRole()) ? "AI" : "USER");
            entry.put("content", msg.getContent());
            history.add(entry);
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(finalSessionId);
        userMsg.setRole("USER");
        userMsg.setContent(request.getQuestion());
        messageService.save(userMsg);
        sessionService.updateTitleIfDefault(finalSessionId, request.getQuestion());

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("userId", userId);
        aiRequest.put("knowledgeBaseId", request.getKnowledgeBaseId());
        aiRequest.put("sessionId", finalSessionId);
        aiRequest.put("question", request.getQuestion());
        aiRequest.put("history", history);

        Map aiResponse = aiServiceClient.askQuestion(aiRequest);

        String answer = "";
        boolean success = false;
        List<Map<String, Object>> citations = new java.util.ArrayList<>();
        int retrievalCount = 0;
        String modelName = "";

        if (aiResponse != null) {
            answer = (String) aiResponse.getOrDefault("answer", "");
            Object successObj = aiResponse.get("success");
            success = successObj instanceof Boolean && (Boolean) successObj;
            Object citeObj = aiResponse.get("citations");
            if (citeObj instanceof List) {
                citations = (List<Map<String, Object>>) citeObj;
            }
            retrievalCount = ((Number) aiResponse.getOrDefault("retrievalCount", 0)).intValue();
            modelName = (String) aiResponse.getOrDefault("modelName", "");
        }

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("AI");
        aiMsg.setContent(answer);
        aiMsg.setModelName(modelName);
        aiMsg.setRetrievalCount(retrievalCount);
        try {
            aiMsg.setCitationJson(objectMapper.writeValueAsString(citations));
        } catch (Exception e) {
            aiMsg.setCitationJson("[]");
        }
        messageService.save(aiMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("answer", answer);
        result.put("citations", citations);
        result.put("retrievalCount", retrievalCount);
        result.put("modelName", modelName);
        result.put("success", success);

        recordFailedQuestionIfNeeded(
                userId,
                request.getKnowledgeBaseId(),
                finalSessionId,
                request.getQuestion(),
                retrievalCount,
                success,
                answer
        );

        return Result.success(result);
    }

    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> askStream(@Valid @RequestBody ChatAskRequest request,
                                                           @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        boolean isAdmin = "ADMIN".equals(userDetails.getRole());

        Long sessionId = request.getSessionId();
        if (sessionId == null) {
            ChatSession session = sessionService.create(userId, request.getKnowledgeBaseId(), isAdmin);
            sessionId = session.getId();
        }
        final Long finalSessionId = sessionId;

        // 加载历史消息（多轮上下文，按时间正序）
        List<ChatMessage> recentMessages = messageService.findRecentBySessionId(finalSessionId, 50);
        List<Map<String, String>> history = new ArrayList<>();
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            Map<String, String> entry = new HashMap<>();
            entry.put("role", "AI".equals(msg.getRole()) ? "AI" : "USER");
            entry.put("content", msg.getContent());
            history.add(entry);
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(finalSessionId);
        userMsg.setRole("USER");
        userMsg.setContent(request.getQuestion());
        messageService.save(userMsg);
        sessionService.updateTitleIfDefault(finalSessionId, request.getQuestion());

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("userId", userId);
        aiRequest.put("knowledgeBaseId", request.getKnowledgeBaseId());
        aiRequest.put("sessionId", finalSessionId);
        aiRequest.put("question", request.getQuestion());
        aiRequest.put("history", history);

        StringBuilder answerBuilder = new StringBuilder();
        List<Map<String, Object>> citations = new ArrayList<>();
        AtomicInteger retrievalCount = new AtomicInteger(0);
        AtomicReference<String> modelName = new AtomicReference<>("");
        AtomicBoolean success = new AtomicBoolean(false);

        StreamingResponseBody stream = outputStream -> {
            String currentEvent = null;
            StringBuilder dataBuilder = new StringBuilder();

            try (BufferedReader reader = aiServiceClient.askQuestionStream(aiRequest)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));

                    if (line.isEmpty()) {
                        handleSseEvent(currentEvent, dataBuilder.toString(), answerBuilder, citations,
                                retrievalCount, modelName, success);
                        currentEvent = null;
                        dataBuilder.setLength(0);
                        outputStream.flush();
                        continue;
                    }

                    if (line.startsWith("event:")) {
                        currentEvent = line.substring(6).trim();
                        continue;
                    }

                    if (line.startsWith("data:")) {
                        if (dataBuilder.length() > 0) {
                            dataBuilder.append("\n");
                        }
                        dataBuilder.append(line.substring(5).trim());
                    }
                }
            } catch (Exception e) {
                String fallbackAnswer = "请求失败，请稍后重试。";
                answerBuilder.setLength(0);
                answerBuilder.append(fallbackAnswer);
                success.set(false);
                String errorEvent = "event: done\ndata: " +
                        objectMapper.writeValueAsString(Map.of(
                                "answer", fallbackAnswer,
                                "citations", citations,
                                "retrievalCount", retrievalCount.get(),
                                "modelName", modelName.get(),
                                "success", false
                        )) + "\n\n";
                outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } finally {
                saveAiMessage(finalSessionId, answerBuilder.toString(), citations, retrievalCount.get(), modelName.get());
                recordFailedQuestionIfNeeded(
                        userId,
                        request.getKnowledgeBaseId(),
                        finalSessionId,
                        request.getQuestion(),
                        retrievalCount.get(),
                        success.get(),
                        answerBuilder.toString()
                );
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }

    private void handleSseEvent(String eventName,
                                String data,
                                StringBuilder answerBuilder,
                                List<Map<String, Object>> citations,
                                AtomicInteger retrievalCount,
                                AtomicReference<String> modelName,
                                AtomicBoolean success) {
        if (eventName == null || data == null || data.isBlank()) {
            return;
        }

        try {
            if ("citations".equals(eventName)) {
                citations.clear();
                citations.addAll(objectMapper.readValue(data, List.class));
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(data, Map.class);
            if ("answer".equals(eventName)) {
                Object text = payload.get("text");
                if (text instanceof String str) {
                    answerBuilder.append(str);
                }
                return;
            }

            if ("done".equals(eventName)) {
                Object answer = payload.get("answer");
                if (answerBuilder.isEmpty() && answer instanceof String str) {
                    answerBuilder.append(str);
                }

                Object payloadCitations = payload.get("citations");
                if (payloadCitations instanceof List<?> list) {
                    citations.clear();
                    citations.addAll((List<Map<String, Object>>) list);
                }

                Object retrieval = payload.get("retrievalCount");
                if (retrieval instanceof Number number) {
                    retrievalCount.set(number.intValue());
                }

                Object model = payload.get("modelName");
                if (model instanceof String str) {
                    modelName.set(str);
                }

                Object doneSuccess = payload.get("success");
                if (doneSuccess instanceof Boolean bool) {
                    success.set(bool);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveAiMessage(Long sessionId,
                               String answer,
                               List<Map<String, Object>> citations,
                               int retrievalCount,
                               String modelName) {
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("AI");
        aiMsg.setContent(answer);
        aiMsg.setModelName(modelName);
        aiMsg.setRetrievalCount(retrievalCount);
        try {
            aiMsg.setCitationJson(objectMapper.writeValueAsString(citations));
        } catch (Exception e) {
            aiMsg.setCitationJson("[]");
        }
        messageService.save(aiMsg);
    }

    private void recordFailedQuestionIfNeeded(Long userId,
                                              Long knowledgeBaseId,
                                              Long sessionId,
                                              String question,
                                              int retrievalCount,
                                              boolean success,
                                              String answer) {
        if (question == null || question.isBlank()) {
            return;
        }

        if (retrievalCount == 0) {
            failedQuestionService.record(
                    userId,
                    knowledgeBaseId,
                    sessionId,
                    question,
                    "NO_HIT",
                    answer
            );
            return;
        }

        if (!success) {
            failedQuestionService.record(
                    userId,
                    knowledgeBaseId,
                    sessionId,
                    question,
                    "MODEL_ERROR",
                    answer
            );
        }
    }
}
