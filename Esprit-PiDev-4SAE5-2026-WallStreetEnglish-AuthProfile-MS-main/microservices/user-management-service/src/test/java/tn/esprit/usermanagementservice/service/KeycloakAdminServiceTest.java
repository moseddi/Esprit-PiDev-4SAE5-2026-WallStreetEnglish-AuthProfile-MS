package tn.esprit.usermanagementservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakAdminService Tests")
class KeycloakAdminServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KeycloakAdminService keycloakAdminService;

    private static final String TEST_EMAIL = "test@test.com";

    // Mock response classes
    private static class TokenResponse {
        public String access_token = "test-admin-token";
        public String getAccessToken() { return access_token; }
    }

    private static class UserResponse {
        public String id = "user-id-123";
        public String getId() { return id; }
    }

    @Nested
    @DisplayName("Logout User Sessions Tests")
    class LogoutUserSessionsTests {

        @Test
        @DisplayName("Should handle logout without throwing when API fails")
        void logoutUserSessions_ShouldNotThrowOnError() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                    .thenThrow(new RuntimeException("Connection failed"));

            assertDoesNotThrow(() -> keycloakAdminService.logoutUserSessions(TEST_EMAIL));
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void logoutUserSessions_WithNullEmail_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.logoutUserSessions(null));
        }

        @Test
        @DisplayName("Should handle empty email gracefully")
        void logoutUserSessions_WithEmptyEmail_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.logoutUserSessions(""));
        }
    }

    @Nested
    @DisplayName("Update User Role Tests")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should handle role update when Keycloak is unavailable")
        void updateUserRole_WhenKeycloakDown_ShouldNotThrow() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                    .thenThrow(new RuntimeException("Keycloak unavailable"));

            assertDoesNotThrow(() -> keycloakAdminService.updateUserRole(TEST_EMAIL, "ADMIN"));
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void updateUserRole_WithNullEmail_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.updateUserRole(null, "ADMIN"));
        }

        @Test
        @DisplayName("Should handle null role gracefully")
        void updateUserRole_WithNullRole_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.updateUserRole(TEST_EMAIL, null));
        }

        @Test
        @DisplayName("Should handle empty role gracefully")
        void updateUserRole_WithEmptyRole_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.updateUserRole(TEST_EMAIL, ""));
        }
    }
}