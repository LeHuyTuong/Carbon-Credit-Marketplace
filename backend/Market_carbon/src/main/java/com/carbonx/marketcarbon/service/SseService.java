package com.carbonx.marketcarbon.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {

    SseEmitter subscribe();
    void sendNotificationToUser(String message);
    void sendNotificationToAll(String message);
}
