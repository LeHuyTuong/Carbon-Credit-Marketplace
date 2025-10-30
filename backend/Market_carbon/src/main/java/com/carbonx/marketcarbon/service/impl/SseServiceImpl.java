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

    private final UserRepository  userRepository;

    // Sử dụng ConcurrentHashMap để map userId (dưới dạng String) với kết nối SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    /**
     * Client đăng ký nhận thông báo.
     * @return SseEmitter để giữ kết nối.
     */
    @Override
    public SseEmitter subscribe() {
        // Tạo emitter với thời gian timeout dài
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // timeout 30 phút

        User currentUser = currentUser();
        Long userId = currentUser.getId();

        // Xóa emitter khỏi map khi hoàn thành, hết hạn hoặc lỗi
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError((e) -> emitters.remove(userId, emitter));


        // Lưu emitter vào map với key là userId
        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            previous.complete();
        }

        // Gửi một sự kiện "init" để xác nhận kết nối
        try{
            emitter.send(SseEmitter.event().name("init").data("Connection established for user " + userId));
        }catch (IOException e){
            // Lỗi thì xóa emitter
            emitters.remove(userId, emitter);
        }
        return emitter;
    }

    /**
     * Gửi thông báo đến một người dùng cụ thể.
     * @param message Nội dung thông báo (có thể là JSON string).
     */
    public void sendNotificationToUser(Long userId,String message) {
        // Lấy emitter của người dùng từ map
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("notification").data(message));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }
    }

    /**
     * Gửi thông báo đến tất cả người dùng đang kết nối (ví dụ: thông báo hệ thống).
     * @param message Nội dung thông báo.
     */
    public void sendNotificationToAll(String message) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
            } catch (IOException e) {
                emitters.remove(userId, emitter);
            }
        });
    }
}
