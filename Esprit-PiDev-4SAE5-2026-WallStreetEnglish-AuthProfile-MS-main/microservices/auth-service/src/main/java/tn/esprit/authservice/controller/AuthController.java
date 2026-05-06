package tn.esprit.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.AuthResponse;
import tn.esprit.authservice.dto.ForgotPasswordRequest;
import tn.esprit.authservice.dto.LoginRequest;
import tn.esprit.authservice.dto.RegisterRequest;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.service.AuthService;
import tn.esprit.authservice.client.UserServiceClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_SUCCESS = "success";

    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserServiceClient userServiceClient;
    private final JavaMailSender mailSender;
    private final Keycloak keycloakAdmin;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${app.mail.from:noreply@wallstreetenglish.com}")
    private String mailFrom;

    /** Thread-safe store for password-reset tokens (token → Keycloak userId). */
    private static final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        try {
            userServiceClient.recordUserLogin(request.getEmail());
            log.info("Login recorded for: {}", request.getEmail());
        } catch (Exception e) {
            log.warn("Failed to record login activity: {}", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth service is working!");
    }

    @PostMapping("/check-password")
    public ResponseEntity<String> checkPassword(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean match = encoder.matches(request.getPassword(), user.getPassword());

        String result = "Password matches: " + match +
                "\nUser active: " + user.isActive() +
                "\nEmail verified: " + user.isEmailVerified();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        try {
            List<UserRepresentation> allUsers = keycloakAdmin.realm(realm).users().list();
            UserRepresentation foundUser = findUserInKeycloak(allUsers, request.getEmail());

            if (foundUser != null) {
                String token = UUID.randomUUID().toString();
                resetTokens.put(token, foundUser.getId());
                String resetLink = "http://192.168.168.128:32708/reset-password?token=" + token;
                sendResetEmail(request.getEmail(), resetLink);
                log.info("Password reset email sent to: {}", request.getEmail());
            } else {
                log.warn("User not found in Keycloak for email: {}", request.getEmail());
            }

        } catch (Exception e) {
            log.error("Error processing forgot password request: {}", e.getMessage(), e);
        }

        Map<String, Object> response = new HashMap<>();
        response.put(FIELD_SUCCESS, true);
        response.put(FIELD_MESSAGE, "If your email exists, you will receive a password reset link.");
        return ResponseEntity.ok(response);
    }

    private UserRepresentation findUserInKeycloak(List<UserRepresentation> users, String email) {
        String searchEmail = email.toLowerCase().trim();
        for (UserRepresentation user : users) {
            if (user.getEmail() != null && user.getEmail().toLowerCase().trim().equals(searchEmail)) {
                return user;
            }
            if (user.getUsername() != null && user.getUsername().toLowerCase().trim().equals(searchEmail)) {
                return user;
            }
        }
        return null;
    }

    private void sendResetEmail(String to, String resetLink) {
        try {
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
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        log.info("Reset password request received");

        String token = request.get("token");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (token == null || newPassword == null || confirmPassword == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(FIELD_MESSAGE, "Missing required fields"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(FIELD_MESSAGE, "Passwords do not match"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(FIELD_MESSAGE, "Password must be at least 6 characters"));
        }

        if (!resetTokens.containsKey(token)) {
            log.warn("Invalid or expired reset token used");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(FIELD_MESSAGE, "Invalid or expired token"));
        }

        String userId = resetTokens.get(token);
        log.info("Valid reset token for user: {}", userId);

        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            keycloakAdmin.realm(realm).users().get(userId).resetPassword(credential);
            resetTokens.remove(token);
            log.info("Password reset successful for user: {}", userId);

            return ResponseEntity.ok(Map.of(
                    FIELD_SUCCESS, true,
                    FIELD_MESSAGE, "Password reset successfully"
            ));

        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(FIELD_MESSAGE, "Error resetting password: " + e.getMessage()));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        log.info("Validating reset token: {}", token);

        if (resetTokens.containsKey(token)) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("valid", false, FIELD_MESSAGE, "Invalid or expired token"));
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(mailFrom);
            message.setSubject("TEST EMAIL");
            message.setText("Email configuration is working correctly.");
            mailSender.send(message);
            return ResponseEntity.ok("Email test sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(
            @RequestParam String email,
            @RequestParam(defaultValue = "VOLUNTARY") String logoutType) {
        log.info("Logout endpoint called: {} ({})", email, logoutType);
        return ResponseEntity.ok(authService.logout(email, logoutType));
    }
}
