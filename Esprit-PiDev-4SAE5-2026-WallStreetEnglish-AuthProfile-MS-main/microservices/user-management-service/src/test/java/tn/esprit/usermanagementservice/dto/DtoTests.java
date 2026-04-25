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
            assertThat(response.getToken()).isEqualTo("token");
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

            assertThat(request.getEmail()).isEqualTo("test@test.com");
            assertThat(request.getFirstName()).isEqualTo("John");
            assertThat(request.getLastName()).isEqualTo("Doe");
            assertThat(request.getRole()).isEqualTo(Role.STUDENT);
            assertThat(request.getCity()).isEqualTo("Tunis");
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

            assertThat(event.getEmail()).isEqualTo("test@test.com");
            assertThat(event.getType()).isEqualTo(LoginEventMessage.EventType.LOGIN);
            assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
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
            dto.setLoginCount(15);
            dto.setDaysSinceLastLogin(2);
            dto.setAverageLoginsPerMonth(5.5);

            assertThat(dto.getEmail()).isEqualTo("test@test.com");
            assertThat(dto.getLoginCount()).isEqualTo(15);
            assertThat(dto.getAverageLoginsPerMonth()).isEqualTo(5.5);
        }
    }
}