package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/ai", "/v1/ai"})
@RequiredArgsConstructor
public class ChatAiController {

    private final GeminiAiService aiService;

    // Body kiểu text/plain: nội dung là câu hỏi của user
    @PreAuthorize("hasRole('COMPANY')")
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody String question) {
        try {
            String answer = aiService.answer(question);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }
}
