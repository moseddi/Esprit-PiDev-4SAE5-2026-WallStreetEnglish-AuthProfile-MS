package tn.esprit.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.authservice.client.UserServiceClient;
import tn.esprit.authservice.config.TestSecurityConfig;
import tn.esprit.authservice.dto.AuthResponse;
import tn.esprit.authservice.dto.LoginRequest;
import tn.esprit.authservice.dto.RegisterRequest;
import tn.esprit.authservice.entity.Role;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.service.AuthService;
import tn.esprit.authservice.service.KeycloakService;
import org.springframework.mail.javamail.JavaMailSender;
import org.keycloak.admin.client.Keycloak;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Extended Tests")
class AuthControllerExtendedTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserServiceClient userServiceClient;
    @MockitoBean private KeycloakService keycloakService;
    @MockitoBean private JavaMailSender mailSender;
    @MockitoBean private Keycloak keycloakAdmin;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        authResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .refreshToken("mock-refresh-token")
                .email("student@test.com")
                .role(Role.STUDENT)
                .userId(1L)
                .message("Success")
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register - Validation")
    class RegisterValidationTests {

        @Test
        @DisplayName("Should return 400 when email is blank")
        void register_BlankEmail_Returns400() throws Exception {
            RegisterRequest bad = new RegisterRequest();
            bad.setEmail("");
            bad.setPassword("password123");
            bad.setConfirmPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void register_InvalidEmailFormat_Returns400() throws Exception {
            RegisterRequest bad = new RegisterRequest();
            bad.setEmail("not-an-email");
            bad.setPassword("password123");
            bad.setConfirmPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void register_ShortPassword_Returns400() throws Exception {
            RegisterRequest bad = new RegisterRequest();
            bad.setEmail("valid@test.com");
            bad.setPassword("abc");
            bad.setConfirmPassword("abc");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void register_BlankPassword_Returns400() throws Exception {
            RegisterRequest bad = new RegisterRequest();
            bad.setEmail("valid@test.com");
            bad.setPassword("");
            bad.setConfirmPassword("");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return correct token in response body")
        void register_ValidRequest_ReturnsTokenInBody() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("valid@test.com");
            req.setPassword("password123");
            req.setConfirmPassword("password123");

            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.email").value("student@test.com"))
                    .andExpect(jsonPath("$.role").value("STUDENT"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login - Validation & Response")
    class LoginTests {

        @Test
        @DisplayName("Should return 400 when login email is blank")
        void login_BlankEmail_Returns400() throws Exception {
            LoginRequest bad = new LoginRequest();
            bad.setEmail("");
            bad.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when login password is blank")
        void login_BlankPassword_Returns400() throws Exception {
            LoginRequest bad = new LoginRequest();
            bad.setEmail("valid@test.com");
            bad.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return token and role in response body")
        void login_ValidCredentials_ReturnsTokenAndRole() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("student@test.com");
            req.setPassword("password123");

            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.role").value("STUDENT"))
                    .andExpect(jsonPath("$.userId").value(1));
        }

        @Test
        @DisplayName("Login still succeeds even when userServiceClient.recordUserLogin throws")
        void login_UserServiceClientThrows_StillReturns200() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("student@test.com");
            req.setPassword("password123");

            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
            // userServiceClient is a mock — by default throws nothing, but simulate failure
            org.mockito.Mockito.doThrow(new RuntimeException("service down"))
                    .when(userServiceClient).recordUserLogin(anyString());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should return 422 when token is missing")
        void resetPassword_MissingToken_Returns400() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("newPassword", "newpass123");
            body.put("confirmPassword", "newpass123");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Missing required fields"));
        }

        @Test
        @DisplayName("Should return 422 when newPassword is missing")
        void resetPassword_MissingNewPassword_Returns400() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("token", "some-token");
            body.put("confirmPassword", "newpass123");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Missing required fields"));
        }

        @Test
        @DisplayName("Should return 422 when passwords do not match")
        void resetPassword_PasswordMismatch_Returns400() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("token", "some-token");
            body.put("newPassword", "password123");
            body.put("confirmPassword", "different456");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Passwords do not match"));
        }

        @Test
        @DisplayName("Should return 422 when password is too short")
        void resetPassword_ShortPassword_Returns400() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("token", "some-token");
            body.put("newPassword", "abc");
            body.put("confirmPassword", "abc");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Password must be at least 6 characters"));
        }

        @Test
        @DisplayName("Should return 422 when token is invalid")
        void resetPassword_InvalidToken_Returns400() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("token", "invalid-token-xyz");
            body.put("newPassword", "newpass123");
            body.put("confirmPassword", "newpass123");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/validate-reset-token")
    class ValidateResetTokenTests {

        @Test
        @DisplayName("Should return invalid=false for unknown token")
        void validateResetToken_UnknownToken_ReturnsInvalid() throws Exception {
            mockMvc.perform(get("/api/auth/validate-reset-token")
                            .param("token", "not-a-real-token"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.valid").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should use default VOLUNTARY logout type when param omitted")
        void logout_NoLogoutTypeParam_DefaultsToVoluntary() throws Exception {
            AuthResponse resp = AuthResponse.builder()
                    .email("student@test.com")
                    .message("Logout successful")
                    .build();
            when(authService.logout("student@test.com", "VOLUNTARY")).thenReturn(resp);

            mockMvc.perform(post("/api/auth/logout")
                            .param("email", "student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful"));
        }

        @Test
        @DisplayName("Should accept TIMEOUT logout type")
        void logout_TimeoutType_Returns200() throws Exception {
            AuthResponse resp = AuthResponse.builder()
                    .email("student@test.com")
                    .message("Logout successful")
                    .build();
            when(authService.logout("student@test.com", "TIMEOUT")).thenReturn(resp);

            mockMvc.perform(post("/api/auth/logout")
                            .param("email", "student@test.com")
                            .param("logoutType", "TIMEOUT"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept FORCED logout type")
        void logout_ForcedType_Returns200() throws Exception {
            AuthResponse resp = AuthResponse.builder()
                    .email("admin@test.com")
                    .message("Logout successful")
                    .build();
            when(authService.logout("admin@test.com", "FORCED")).thenReturn(resp);

            mockMvc.perform(post("/api/auth/logout")
                            .param("email", "admin@test.com")
                            .param("logoutType", "FORCED"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password - Validation")
    class ForgotPasswordValidationTests {

        @Test
        @DisplayName("Should return 400 when email is blank")
        void forgotPassword_BlankEmail_Returns400() throws Exception {
            Map<String, String> body = Map.of("email", "");

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void forgotPassword_InvalidEmail_Returns400() throws Exception {
            Map<String, String> body = Map.of("email", "notanemail");

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should always return success=true even for unknown email (security)")
        void forgotPassword_UnknownEmail_ReturnsSuccessAnyway() throws Exception {
            Map<String, String> body = Map.of("email", "ghost@test.com");

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/check-password")
    class CheckPasswordTests {

        @Test
        @DisplayName("Should return password match info for existing user")
        void checkPassword_ExistingUser_ReturnsMatchInfo() throws Exception {
            User user = User.builder()
                    .email("student@test.com")
                    .password("$2a$10$somehashedpassword")
                    .role(Role.STUDENT)
                    .active(true)
                    .emailVerified(true)
                    .build();
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));

            LoginRequest req = new LoginRequest();
            req.setEmail("student@test.com");
            req.setPassword("password123");

            mockMvc.perform(post("/api/auth/check-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Password matches:")));
        }
    }
}