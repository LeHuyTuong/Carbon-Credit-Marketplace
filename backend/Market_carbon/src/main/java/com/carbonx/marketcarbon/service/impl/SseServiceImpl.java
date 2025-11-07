package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.UserRepository;
import com.carbonx.marketcarbon.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    private final UserRepository userRepository;

    // Lưu danh sách emitter cho từng user đang kết nối
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Lấy thông tin user hiện tại từ SecurityContext
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new ResourceNotFoundException("User not authenticated for SSE subscription");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    /**
     * Client đăng ký nhận thông báo (kết nối SSE)
     */
    @Override
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // timeout 30 phút

        User currentUser = currentUser();
        Long userId = currentUser.getId();

        // Dọn emitter cũ nếu tồn tại
        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) previous.complete();

        // Khi lỗi hoặc timeout thì remove emitter
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError((e) -> emitters.remove(userId, emitter));

        // Gửi sự kiện init
        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("Connection established for user " + userId));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }

        return emitter;
    }

    /**
     * Gửi thông báo đến 1 user cụ thể
     */
    public void sendNotificationToUser(Long userId, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(message));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }
    }

    /**
     * Gửi thông báo đến tất cả người đang online
     */
    public void sendNotificationToAll(String message) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            } catch (IOException e) {
                emitters.remove(userId, emitter);
            }
        });
    }

    /**
     * Trả về danh sách userId đang kết nối (để broadcast theo role)
     */
    public Map<Long, SseEmitter> getEmitters() {
        return emitters;
    }
}
