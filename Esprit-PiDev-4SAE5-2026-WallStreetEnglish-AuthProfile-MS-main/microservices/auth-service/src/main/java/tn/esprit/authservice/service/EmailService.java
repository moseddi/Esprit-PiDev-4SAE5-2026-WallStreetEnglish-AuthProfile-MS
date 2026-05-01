package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@wallstreetenglish.com}")
    private String mailFrom;

    public void sendResetLink(String to, String resetLink) {
        try {
            log.info("Sending password reset email to: {}", to);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("Reset your Wall Street English password");
            message.setText(
                    "Hello,\n\n" +
                            "You requested to reset your password.\n\n" +
                            "Click this link to reset your password: " + resetLink + "\n\n" +
                            "This link will expire in 24 hours.\n\n" +
                            "If you didn't request this, ignore this email.\n\n" +
                            "Wall Street English Team"
            );

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
        }
    }
}