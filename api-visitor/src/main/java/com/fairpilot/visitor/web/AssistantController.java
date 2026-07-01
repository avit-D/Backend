package com.fairpilot.visitor.web;

import com.fairpilot.dto.AssistantDto.*;
import com.fairpilot.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/assistant/ask
 *
 * 요청 예시:
 * {
 *   "question": "보안 관련 부스 어디 있어?",
 *   "candidateBoothIds": ["BOOTH-003", "BOOTH-004"]   ← 선택
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        log.info("[AssistantController] 질문 수신: '{}'", request.getQuestion());
        return ResponseEntity.ok(assistantService.ask(request));
    }
}