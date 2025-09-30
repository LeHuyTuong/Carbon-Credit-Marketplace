    package com.carbonx.marketcarbon.service.impl;

    import com.carbonx.marketcarbon.config.JwtProvider;
    import com.carbonx.marketcarbon.exception.UserException;
    import com.carbonx.marketcarbon.model.PasswordResetToken;
    import com.carbonx.marketcarbon.model.User;
    import com.carbonx.marketcarbon.repository.PasswordResetTokenRepository;
    import com.carbonx.marketcarbon.repository.UserRepository;
    import com.carbonx.marketcarbon.service.UserService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.mail.SimpleMailMessage;
    import org.springframework.mail.javamail.JavaMailSender;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;

    import java.util.Calendar;
    import java.util.Date;
    import java.util.List;
    import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    public class UserServiceImplementation implements UserService {

        private final UserRepository userRepository;
        private final JwtProvider jwtProvider;
        private final PasswordEncoder passwordEncoder;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final JavaMailSender javaMailSender;


        @Override
        public User findUserProfileByJwt(String jwt) throws UserException {
            String email=jwtProvider.getEmailFromJwtToken(jwt);


            User user = userRepository.findByEmail(email);

            if(user==null) {
                throw new UserException("user not exist with email "+email);
            }
//		System.out.println("email user "+user.get().getEmail());
            return user;
        }

        @Override
        public User findUserByEmail(String username) throws UserException {

            User user=userRepository.findByEmail(username);

            if(user!=null) {

                return user;
            }

            throw new UserException("user not exist with username "+username);
        }

        @Override
        public List<User> findALlUser() {
            return userRepository.findAll();
        }

        @Override
        public void updatePassword(User user, String newPassword) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }

        @Override
        public void sendPasswordResetEmail(User user) {
            // Generate a random token (you might want to use a library for this)
            String resetToken = generateRandomToken();

            // Calculate expiry date
            Date expiryDate = calculateExpiryDate();

            // Save the token in the database
            PasswordResetToken passwordResetToken = new PasswordResetToken(resetToken,user,expiryDate);
            passwordResetTokenRepository.save(passwordResetToken);

            // Send an email containing the reset link
            sendEmail(user.getEmail(), "Password Reset", "Click the following link to reset your password: http://localhost:3000/account/reset-password?token=" + resetToken);
        }
        private void sendEmail(String to, String subject, String message) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            javaMailSender.send(mailMessage);
        }
        private String generateRandomToken() {
            return UUID.randomUUID().toString();
        }
        private Date calculateExpiryDate() {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MINUTE, 10);
            return cal.getTime();

        }
    }
