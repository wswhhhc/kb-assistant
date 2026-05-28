package com.example.kbassistant.controller;

import com.example.kbassistant.client.AiServiceClient;
import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.ChatAskRequest;
import com.example.kbassistant.dto.response.ChatAskContext;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.ChatAskService;
import com.example.kbassistant.service.ChatMessageService;
import com.example.kbassistant.service.ChatSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final ChatAskService chatAskService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    @PostMapping("/sessions")
    public Result<?> createSession(@RequestParam Long knowledgeBaseId,
                                   @AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(sessionService.create(userDetails.getUserId(), knowledgeBaseId, userDetails.isAdmin()));
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
        ChatAskContext ctx = chatAskService.prepareContext(
                userId, request.getKnowledgeBaseId(), request.getSessionId(),
                request.getQuestion(), userDetails.isAdmin());

        Map<String, Object> aiRequest = buildAiRequest(userId, request, ctx);
        Map aiResponse = aiServiceClient.askQuestion(aiRequest);

        String answer = "";
        boolean success = false;
        List<Map<String, Object>> citations = new ArrayList<>();
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

        chatAskService.saveAiMessage(ctx.getSessionId(), answer, citations, retrievalCount, modelName);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", ctx.getSessionId());
        result.put("answer", answer);
        result.put("citations", citations);
        result.put("retrievalCount", retrievalCount);
        result.put("modelName", modelName);
        result.put("success", success);

        chatAskService.recordFailedQuestion(
                userId, request.getKnowledgeBaseId(), ctx.getSessionId(),
                request.getQuestion(), retrievalCount, success, answer);

        return Result.success(result);
    }

    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> askStream(@Valid @RequestBody ChatAskRequest request,
                                                           @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        ChatAskContext ctx = chatAskService.prepareContext(
                userId, request.getKnowledgeBaseId(), request.getSessionId(),
                request.getQuestion(), userDetails.isAdmin());

        Map<String, Object> aiRequest = buildAiRequest(userId, request, ctx);

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
                chatAskService.saveAiMessage(ctx.getSessionId(), answerBuilder.toString(), citations, retrievalCount.get(), modelName.get());
                chatAskService.recordFailedQuestion(
                        userId, request.getKnowledgeBaseId(), ctx.getSessionId(),
                        request.getQuestion(), retrievalCount.get(), success.get(), answerBuilder.toString());
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }

    private Map<String, Object> buildAiRequest(Long userId, ChatAskRequest request, ChatAskContext ctx) {
        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("userId", userId);
        aiRequest.put("knowledgeBaseId", request.getKnowledgeBaseId());
        aiRequest.put("sessionId", ctx.getSessionId());
        aiRequest.put("question", request.getQuestion());
        aiRequest.put("history", ctx.getHistory());
        return aiRequest;
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
        } catch (Exception e) {
            log.warn("SSE 事件解析失败: event={}, data={}", eventName, data, e);
        }
    }
}
