package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String emailHost;

    @Autowired
    private JavaMailSender javaMailSender;
    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendEmail(String email, String otp) {
        log.info("Sending mail to {}", email);
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(emailHost);
        message.setTo(email);
        message.setSubject("Your OTP Code");

        //B2 : check là OTP hay là noti
        // Trường hợp gửi thông tin về nhiều email cùng lúc như thông báo
//        if(email.contains(",")){
//            message.setTo(InternetAddress.parse(email));
//        }else{
//            message.setTo(email);
//        }

        //B3 : set data vào email
        message.setSubject("Your OTP Code: ");
        message.setText(otp);

        try{
            javaMailSender.send(message);
            log.info("Mail sent to {}, otp is {}" , email, otp);

        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // Generates 6-digit OTP
        return String.valueOf(otp);
    }

    @Override
    public String encodeOtp(String otp) {
        BCryptPasswordEncoder otpEncoder = new BCryptPasswordEncoder();
        return otpEncoder.encode(otp);
    }
}
