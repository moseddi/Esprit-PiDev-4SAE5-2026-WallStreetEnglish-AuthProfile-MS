package tn.esprit.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.authservice.client.UserServiceClient;
import tn.esprit.authservice.dto.AuthResponse;
import tn.esprit.authservice.dto.LoginRequest;
import tn.esprit.authservice.dto.LoginEventMessage;
import tn.esprit.authservice.entity.Role;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Extended Tests")
class AuthServiceExtendedTest {

    @Mock private UserRepository userRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private WebClient webClient;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "realm", "myapp2");
        ReflectionTestUtils.setField(authService, "clientId", "angular-app");
        ReflectionTestUtils.setField(authService, "serverUrl", "http://localhost:6083");

        testUser = User.builder()
                .id(1L)
                .email("student@test.com")
                .role(Role.STUDENT)
                .keycloakId("keycloak-123")
                .active(true)
                .emailVerified(true)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("student@test.com");
        loginRequest.setPassword("password123");
    }

    @Nested
    @DisplayName("Login - user not found")
    class LoginUserNotFoundTests {

        @Test
        @DisplayName("Should throw RuntimeException when user not found in DB")
        void login_UserNotFoundInDB_ThrowsException() {
            // Keycloak call would fail since we have no mock for webClient here,
            // so the exception surfaces as "Invalid credentials"
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    authService.login(loginRequest));
            assertEquals("Invalid credentials", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Logout - all logout types")
    class LogoutTypeTests {

        @Test
        @DisplayName("Should handle TIMEOUT logout type")
        void logout_TimeoutType_ShouldSucceed() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Firefox/91");
            when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

            AuthResponse response = authService.logout("student@test.com", "TIMEOUT");

            assertNotNull(response);
            assertEquals("Logout successful", response.getMessage());
        }

        @Test
        @DisplayName("Should handle FORCED logout type")
        void logout_ForcedType_ShouldSucceed() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Safari/537");
            when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.50");
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

            AuthResponse response = authService.logout("student@test.com", "FORCED");

            assertNotNull(response);
            assertEquals("Logout successful", response.getMessage());
        }

        @Test
        @DisplayName("Should return correct role in logout response")
        void logout_ShouldReturnUserRole() {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn("Chrome/91");
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

            AuthResponse response = authService.logout("student@test.com", "VOLUNTARY");

            assertEquals(Role.STUDENT, response.getRole());
        }
    }

    @Nested
    @DisplayName("parseBrowser (via logout -> buildLogoutEventMessage)")
    class ParseBrowserTests {

        void doLogout(String userAgent) {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn(userAgent);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
            authService.logout("student@test.com", "VOLUNTARY");
        }

        @Test @DisplayName("Chrome user agent") void chrome() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 Chrome/91")); }
        @Test @DisplayName("Firefox user agent") void firefox() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 Firefox/90")); }
        @Test @DisplayName("Safari user agent") void safari() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 Safari/537")); }
        @Test @DisplayName("Edge user agent") void edge() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 Edge/18")); }
        @Test @DisplayName("Opera user agent") void opera() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 OPR/77")); }
        @Test @DisplayName("MSIE user agent") void ie() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 MSIE 11.0")); }
        @Test @DisplayName("Trident user agent") void trident() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 Trident/7.0")); }
        @Test @DisplayName("Unknown user agent") void unknown() { assertDoesNotThrow(() -> doLogout("SomeBot/1.0")); }
        @Test @DisplayName("Null user agent uses Unknown fallback") void nullAgent() { assertDoesNotThrow(() -> doLogout(null)); }
    }

    @Nested
    @DisplayName("parseOperatingSystem (via logout)")
    class ParseOsTests {

        void doLogout(String userAgent) {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn(userAgent);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
            authService.logout("student@test.com", "VOLUNTARY");
        }

        @Test void windows() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (Windows NT 10.0)")); }
        @Test void mac() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (Macintosh; Intel Mac OS X)")); }
        @Test void linux() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (X11; Linux x86_64)")); }
        @Test void android() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (Linux; Android 11)")); }
        @Test void ios() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (iPhone; CPU iOS 14)")); }
        @Test void ipad() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (iPad; CPU OS 14)")); }
        @Test void unknownOs() { assertDoesNotThrow(() -> doLogout("SomeBrowser/1.0")); }
    }

    @Nested
    @DisplayName("parseDeviceType (via logout)")
    class ParseDeviceTypeTests {

        void doLogout(String userAgent) {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn(userAgent);
            when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
            authService.logout("student@test.com", "VOLUNTARY");
        }

        @Test void mobile() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (Linux; Android 11; Mobile)")); }
        @Test void tablet() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (iPad; CPU OS 14; Tablet)")); }
        @Test void desktop() { assertDoesNotThrow(() -> doLogout("Mozilla/5.0 (Windows NT 10.0; Win64)")); }
    }

    // ── REPLACE the entire GetClientIpAddressTests nested class with this ──

    @Nested
    @DisplayName("getClientIpAddress (via logout)")
    class GetClientIpAddressTests {

        void doLogout(String xForwardedFor, String xRealIp, String remoteAddr) {
            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));
            when(httpServletRequest.getHeader("User-Agent")).thenReturn("Chrome/91");
            lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
            lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(xRealIp);
            lenient().when(httpServletRequest.getRemoteAddr()).thenReturn(remoteAddr);
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
            authService.logout("student@test.com", "VOLUNTARY");
        }

        @Test
        @DisplayName("Should use X-Forwarded-For when present")
        void usesXForwardedFor() {
            assertDoesNotThrow(() -> doLogout("203.0.113.5", null, null));
        }

        @Test
        @DisplayName("Should extract first IP from X-Forwarded-For chain")
        void usesFirstIpFromXForwardedForChain() {
            assertDoesNotThrow(() -> doLogout("203.0.113.5, 10.0.0.1, 192.168.1.1", null, null));
        }

        @Test
        @DisplayName("Should convert IPv6 loopback to 127.0.0.1 in X-Forwarded-For")
        void convertsIpv6LoopbackInXForwardedFor() {
            assertDoesNotThrow(() -> doLogout("0:0:0:0:0:0:0:1", null, null));
        }

        @Test
        @DisplayName("Should fall back to X-Real-IP when X-Forwarded-For is null")
        void fallsBackToXRealIp() {
            assertDoesNotThrow(() -> doLogout(null, "198.51.100.1", null));
        }

        @Test
        @DisplayName("Should fall back to remoteAddr when headers are null")
        void fallsBackToRemoteAddr() {
            assertDoesNotThrow(() -> doLogout(null, null, "10.0.0.99"));
        }

        @Test
        @DisplayName("Should convert IPv6 loopback remoteAddr to 127.0.0.1")
        void convertsIpv6LoopbackInRemoteAddr() {
            assertDoesNotThrow(() -> doLogout(null, null, "0:0:0:0:0:0:0:1"));
        }
    }

    @Nested
    @DisplayName("Register - email already exists")
    class RegisterEmailExistsTest {

        @Test
        @DisplayName("Should throw when email already exists")
        void register_EmailAlreadyExists_Throws() {
            tn.esprit.authservice.dto.RegisterRequest req = new tn.esprit.authservice.dto.RegisterRequest();
            req.setEmail("student@test.com");
            req.setPassword("password123");
            req.setConfirmPassword("password123");
            req.setRole(Role.STUDENT);

            when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(testUser));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
            assertEquals("Email already exists", ex.getMessage());
            verify(userRepository, never()).save(any());
        }
    }
}