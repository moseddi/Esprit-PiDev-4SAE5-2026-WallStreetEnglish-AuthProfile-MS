package tn.esprit.usermanagementservice.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tn.esprit.usermanagementservice.entity.Role;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO Tests")
class DtoTests {

    @Nested
    @DisplayName("AuthRegisterRequest Tests")
    class AuthRegisterRequestTests {

        @Test
        void shouldCreateRequestWithAllFields() {
            AuthRegisterRequest request = new AuthRegisterRequest();
            request.setEmail("test@test.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");
            request.setRole("STUDENT");

            assertThat(request.getEmail()).isEqualTo("test@test.com");
            assertThat(request.getPassword()).isEqualTo("password123");
            assertThat(request.getConfirmPassword()).isEqualTo("password123");
            assertThat(request.getRole()).isEqualTo("STUDENT");
        }
    }

    @Nested
    @DisplayName("AuthResponse Tests")
    class AuthResponseTests {

        @Test
        void shouldCreateResponseWithBuilder() {
            AuthResponse response = AuthResponse.builder()
                    .token("jwt-token-123")
                    .email("test@test.com")
                    .role(Role.STUDENT)
                    .userId(1L)
                    .message("Success")
                    .build();

            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getEmail()).isEqualTo("test@test.com");
            assertThat(response.getRole()).isEqualTo(Role.STUDENT);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getMessage()).isEqualTo("Success");
        }

        @Test
        void shouldCreateResponseWithNoArgsConstructor() {
            AuthResponse response = new AuthResponse();
            response.setToken("token");
            response.setEmail("test@test.com");
            response.setRole(Role.ADMIN);
            response.setUserId(2L);
            response.setMessage("OK");

            assertThat(response.getToken()).isEqualTo("token");
            assertThat(response.getEmail()).isEqualTo("test@test.com");
            assertThat(response.getRole()).isEqualTo(Role.ADMIN);
            assertThat(response.getUserId()).isEqualTo(2L);
            assertThat(response.getMessage()).isEqualTo("OK");
        }
    }

    @Nested
    @DisplayName("CreateUserRequest Tests")
    class CreateUserRequestTests {

        @Test
        void shouldCreateRequestWithAllFields() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("test@test.com");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setRole(Role.STUDENT);
            request.setCity("Tunis");
            request.setCountry("Tunisia");
            request.setPhoneNumber("12345678");
            request.setAddress("123 Main St");

            assertThat(request.getEmail()).isEqualTo("test@test.com");
            assertThat(request.getFirstName()).isEqualTo("John");
            assertThat(request.getLastName()).isEqualTo("Doe");
            assertThat(request.getRole()).isEqualTo(Role.STUDENT);
            assertThat(request.getCity()).isEqualTo("Tunis");
            assertThat(request.getCountry()).isEqualTo("Tunisia");
        }
    }

    @Nested
    @DisplayName("UpdateUserRequest Tests")
    class UpdateUserRequestTests {

        @Test
        void shouldCreateRequestWithAllFields() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("Jane");
            request.setLastName("Smith");
            request.setRole(Role.TUTOR);
            request.setActive(true);
            request.setCity("Sfax");
            request.setCountry("Tunisia");
            request.setPhoneNumber("987654321");

            assertThat(request.getFirstName()).isEqualTo("Jane");
            assertThat(request.getLastName()).isEqualTo("Smith");
            assertThat(request.getRole()).isEqualTo(Role.TUTOR);
            assertThat(request.getActive()).isTrue();
            assertThat(request.getCity()).isEqualTo("Sfax");
        }
    }

    @Nested
    @DisplayName("UserProfileDTO Tests")
    class UserProfileDTOTests {

        @Test
        void shouldCreateDTOWithAllFields() {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(1L);
            dto.setEmail("test@test.com");
            dto.setFirstName("John");
            dto.setLastName("Doe");
            dto.setRole(Role.STUDENT);
            dto.setActive(true);
            dto.setBlocked(false);
            dto.setLoginCount(10);
            dto.setCity("Tunis");
            dto.setCountry("Tunisia");
            dto.setPhoneNumber("12345678");

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getEmail()).isEqualTo("test@test.com");
            assertThat(dto.getFirstName()).isEqualTo("John");
            assertThat(dto.getRole()).isEqualTo(Role.STUDENT);
            assertThat(dto.isActive()).isTrue();
            assertThat(dto.isBlocked()).isFalse();
            assertThat(dto.getLoginCount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("LoginEventMessage Tests")
    class LoginEventMessageTests {

        @Test
        void shouldCreateEventWithAllFields() {
            LoginEventMessage event = new LoginEventMessage();
            event.setEmail("test@test.com");
            event.setRole("STUDENT");
            event.setType(LoginEventMessage.EventType.LOGIN);
            event.setIpAddress("127.0.0.1");
            event.setBrowser("Chrome");
            event.setOs("Windows");
            event.setDeviceType("Desktop");
            event.setSessionId("session-123");

            assertThat(event.getEmail()).isEqualTo("test@test.com");
            assertThat(event.getType()).isEqualTo(LoginEventMessage.EventType.LOGIN);
            assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(event.getBrowser()).isEqualTo("Chrome");
        }

        @Test
        void shouldHandleLogoutEvent() {
            LoginEventMessage event = new LoginEventMessage();
            event.setEmail("test@test.com");
            event.setType(LoginEventMessage.EventType.LOGOUT);
            event.setLogoutType(LoginEventMessage.LogoutType.VOLUNTARY);

            assertThat(event.getType()).isEqualTo(LoginEventMessage.EventType.LOGOUT);
            assertThat(event.getLogoutType()).isEqualTo(LoginEventMessage.LogoutType.VOLUNTARY);
        }
    }

    @Nested
    @DisplayName("RoleUpdateRequest Tests")
    class RoleUpdateRequestTests {

        @Test
        void shouldCreateRoleUpdateRequest() {
            RoleUpdateRequest request = new RoleUpdateRequest();
            request.setEmail("test@test.com");
            request.setRole(Role.ADMIN);

            assertThat(request.getEmail()).isEqualTo("test@test.com");
            assertThat(request.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("UserActivityDTO Tests")
    class UserActivityDTOTests {

        @Test
        void shouldCreateUserActivityDTO() {
            UserActivityDTO dto = new UserActivityDTO();
            dto.setId(1L);
            dto.setEmail("test@test.com");
            dto.setFirstName("John");
            dto.setLastName("Doe");
            dto.setRole("STUDENT");
            dto.setCity("Tunis");
            dto.setLoginCount(15);
            dto.setDaysSinceLastLogin(2);
            dto.setAverageLoginsPerMonth(5.5);

            assertThat(dto.getEmail()).isEqualTo("test@test.com");
            assertThat(dto.getLoginCount()).isEqualTo(15);
            assertThat(dto.getAverageLoginsPerMonth()).isEqualTo(5.5);
            assertThat(dto.getDaysSinceLastLogin()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("UserActivityStatsDTO Tests")
    class UserActivityStatsDTOTests {

        @Test
        void shouldCreateUserActivityStatsDTO() {
            UserActivityStatsDTO stats = new UserActivityStatsDTO();
            stats.setTotalUsers(100);
            stats.setActiveUsersLastDay(50);
            stats.setRetentionRate(75.5);
            stats.setAverageLoginsPerUser(3.2);

            assertThat(stats.getTotalUsers()).isEqualTo(100);
            assertThat(stats.getActiveUsersLastDay()).isEqualTo(50);
            assertThat(stats.getRetentionRate()).isEqualTo(75.5);
            assertThat(stats.getAverageLoginsPerUser()).isEqualTo(3.2);
        }
    }
}