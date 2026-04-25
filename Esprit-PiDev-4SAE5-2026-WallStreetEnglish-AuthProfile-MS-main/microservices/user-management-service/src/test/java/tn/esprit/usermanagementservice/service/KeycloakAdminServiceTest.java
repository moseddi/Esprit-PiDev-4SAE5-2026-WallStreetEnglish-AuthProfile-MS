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
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakAdminService Tests")
class KeycloakAdminServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KeycloakAdminService keycloakAdminService;

    private static final String TEST_EMAIL = "test@test.com";

    @Nested
    @DisplayName("Logout User Sessions Tests")
    class LogoutUserSessionsTests {

        @Test
        @DisplayName("Should handle logout without throwing exceptions")
        void logoutUserSessions_ShouldHandleGracefully() {
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

        @Test
        @DisplayName("Should handle non-existent user gracefully")
        void logoutUserSessions_NonExistentUser_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.logoutUserSessions("nonexistent@test.com"));
        }
    }

    @Nested
    @DisplayName("Update User Role Tests")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should handle role update without throwing")
        void updateUserRole_ShouldNotThrow() {
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

        @Test
        @DisplayName("Should handle non-existent user gracefully")
        void updateUserRole_NonExistentUser_ShouldNotThrow() {
            assertDoesNotThrow(() -> keycloakAdminService.updateUserRole("nonexistent@test.com", "ADMIN"));
        }
    }
}