package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseService sseService;

    @Operation(summary = "API to get notification by User ID  " , description = "Get notification for  ")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeNotification() {
        return sseService.subscribe();
    }
}
