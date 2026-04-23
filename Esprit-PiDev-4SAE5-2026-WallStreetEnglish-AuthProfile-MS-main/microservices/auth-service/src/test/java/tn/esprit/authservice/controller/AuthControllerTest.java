package tn.esprit.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import tn.esprit.authservice.dto.ForgotPasswordRequest;
import tn.esprit.authservice.dto.LoginRequest;
import tn.esprit.authservice.dto.RegisterRequest;
import tn.esprit.authservice.entity.Role;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.service.AuthService;
import tn.esprit.authservice.service.KeycloakService;
import org.springframework.mail.javamail.JavaMailSender;
import org.keycloak.admin.client.Keycloak;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserServiceClient userServiceClient;
    @MockitoBean private KeycloakService keycloakService;
    @MockitoBean private JavaMailSender mailSender;
    @MockitoBean private Keycloak keycloakAdmin;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private ForgotPasswordRequest forgotPasswordRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("student@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        registerRequest.setRole(Role.STUDENT);
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("student@test.com");
        loginRequest.setPassword("password123");

        forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("student@test.com");

        authResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .refreshToken("mock-refresh-token")
                .email("student@test.com")
                .role(Role.STUDENT)
                .userId(1L)
                .message("Success")
                .build();
    }

    @Test
    void register_ValidRequest_Returns200() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void login_ValidCredentials_Returns200() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_ValidEmail_Returns200() throws Exception {
        AuthResponse logoutResponse = AuthResponse.builder()
                .email("student@test.com")
                .message("Logout successful")
                .build();
        when(authService.logout("student@test.com", "VOLUNTARY")).thenReturn(logoutResponse);
        mockMvc.perform(post("/api/auth/logout")
                        .param("email", "student@test.com")
                        .param("logoutType", "VOLUNTARY"))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_ValidEmail_Returns200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk());
    }
}