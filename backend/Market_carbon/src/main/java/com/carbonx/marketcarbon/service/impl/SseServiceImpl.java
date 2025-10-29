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

    private User currentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return user;
    }


    // Sử dụng ConcurrentHashMap để map userId (dưới dạng String) với kết nối SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Client đăng ký nhận thông báo.
     * @return SseEmitter để giữ kết nối.
     */
    @Override
    public SseEmitter subscribe() {
        // Tạo emitter với thời gian timeout dài
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        User currentUser = currentUser();

        // Xóa emitter khỏi map khi hoàn thành, hết hạn hoặc lỗi
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        // Lưu emitter vào map với key là userId
        this.emitters.put(currentUser().getId(), emitter);

        // Gửi một sự kiện "init" để xác nhận kết nối
        try{
            emitter.send(SseEmitter.event().name("init").data("Connection established for user " + currentUser.getId()));
        }catch (IOException e){
            // Lỗi thì xóa emitter
            this.emitters.remove(emitter);
        }
        return  emitter;
    }

    /**
     * Gửi thông báo đến một người dùng cụ thể.
     * @param message Nội dung thông báo (có thể là JSON string).
     */
    public void sendNotificationToUser(String message) {
        // Lấy emitter của người dùng từ map
        User  currentUser = currentUser();
        SseEmitter emitter = emitters.get(currentUser().getId());
        if (emitter != null) {
            try {
                // Gửi sự kiện với tên là "notification" và data là message
                emitter.send(SseEmitter.event().name("notification").data(message));
            } catch (IOException e) {
                // Nếu lỗi (ví dụ: client ngắt kết nối), xóa emitter khỏi map
                emitters.remove(currentUser().getId());
            }
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
                emitters.remove(userId);
            }
        });
    }
}
