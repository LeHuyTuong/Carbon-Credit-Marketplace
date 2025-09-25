package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.service.EmailService;
import jakarta.validation.constraints.Email;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(classes = com.carbonx.marketcarbon.service.impl.EmailServiceImpl.class)

public class SendEmailTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void sendEmailTest(){
        String email = "dihoc77@gmail.com";
        String otp = emailService.generateOtp();
        emailService.sendEmail(email,otp);
    }

}
