package tn.esprit.usermanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import tn.esprit.usermanagementservice.config.TestSecurityConfig;
import tn.esprit.usermanagementservice.dto.CreateUserRequest;
import tn.esprit.usermanagementservice.dto.UpdateUserRequest;
import tn.esprit.usermanagementservice.dto.UserProfileDTO;
import tn.esprit.usermanagementservice.entity.Role;
import tn.esprit.usermanagementservice.repository.LoginHistoryRepository;
import tn.esprit.usermanagementservice.repository.UserProfileRepository;
import tn.esprit.usermanagementservice.service.KeycloakAdminService;
import tn.esprit.usermanagementservice.service.UserProfileService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("UserProfileController Tests")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private KeycloakAdminService keycloakAdminService;

    @MockitoBean
    private LoginHistoryRepository loginHistoryRepository;

    @MockitoBean
    private UserProfileRepository userProfileRepository;

    // ==================== GET ENDPOINT TESTS ====================

    @Nested
    @DisplayName("GET Endpoints Tests")
    class GetEndpointTests {

        @Test
        @DisplayName("GET /api/users/{id} - Should return user by ID")
        void getUserById_ShouldReturnUser() throws Exception {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(1L);
            dto.setEmail("test@test.com");
            dto.setRole(Role.STUDENT);

            when(userProfileService.getUserById(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@test.com"));
        }

        @Test
        @DisplayName("GET /api/users/email/{email} - Should return user by email")
        void getUserByEmail_ShouldReturnUser() throws Exception {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setEmail("test@test.com");
            dto.setRole(Role.STUDENT);

            when(userProfileService.getUserByEmail("test@test.com")).thenReturn(dto);

            mockMvc.perform(get("/api/users/email/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("test@test.com"));
        }

        @Test
        @DisplayName("GET /api/users - Should return all users")
        void getAllUsers_ShouldReturnList() throws Exception {
            when(userProfileService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/role/{role} - Should return users by role")
        void getUsersByRole_ShouldReturnFilteredList() throws Exception {
            when(userProfileService.getUsersByRole(Role.STUDENT)).thenReturn(List.of());

            mockMvc.perform(get("/api/users/role/STUDENT"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/test - Should return working message")
        void testEndpoint_ShouldReturnWorkingMessage() throws Exception {
            mockMvc.perform(get("/api/users/test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User Management Service is working!"));
        }
    }

    // ==================== POST CREATE ENDPOINT TESTS ====================

    @Nested
    @DisplayName("POST Create Endpoint Tests")
    class PostCreateTests {

        @Test
        @DisplayName("POST /api/users - Should create user")
        void createUser_ShouldReturnOk() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("new@test.com");
            request.setRole(Role.STUDENT);
            request.setFirstName("John");
            request.setLastName("Doe");

            UserProfileDTO dto = new UserProfileDTO();
            dto.setEmail("new@test.com");
            dto.setRole(Role.STUDENT);

            when(userProfileService.createUser(any(CreateUserRequest.class), anyString()))
                    .thenReturn(dto);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("new@test.com"));
        }

        @Test
        @DisplayName("POST /api/users/record-login - Should record user login")
        void recordUserLogin_ShouldReturnOk() throws Exception {
            doNothing().when(userProfileService).recordUserLogin("test@test.com");

            mockMvc.perform(post("/api/users/record-login")
                            .param("email", "test@test.com"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/users/record-logout - Should record user logout")
        void recordUserLogout_ShouldReturnOk() throws Exception {
            doNothing().when(userProfileService).recordUserLogout(anyString(), any());

            mockMvc.perform(post("/api/users/record-logout")
                            .param("email", "test@test.com")
                            .param("logoutType", "VOLUNTARY"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/users/force-logout/{email} - Should force logout user")
        void forceLogout_ShouldReturnOk() throws Exception {
            doNothing().when(keycloakAdminService).logoutUserSessions("test@test.com");
            doNothing().when(userProfileService).recordUserLogout(anyString(), any());

            mockMvc.perform(post("/api/users/force-logout/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User test@test.com logged out"));
        }

        @Test
        @DisplayName("POST /api/users/sync-from-auth - Should sync user from auth")
        void syncFromAuth_ShouldReturnOk() throws Exception {
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", "test@test.com");
            userData.put("role", "STUDENT");

            UserProfileDTO dto = new UserProfileDTO();
            dto.setEmail("test@test.com");

            when(userProfileService.syncUserFromAuth(eq("test@test.com"), eq("STUDENT")))
                    .thenReturn(dto);

            mockMvc.perform(post("/api/users/sync-from-auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userData)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/users/block/{email} - Should block user")
        void blockUser_ShouldReturnOk() throws Exception {
            doNothing().when(userProfileService).blockUser("test@test.com", "Suspicious activity");

            mockMvc.perform(post("/api/users/block/test@test.com")
                            .param("reason", "Suspicious activity"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User test@test.com has been blocked"));
        }

        @Test
        @DisplayName("POST /api/users/unblock/{email} - Should unblock user")
        void unblockUserPost_ShouldReturnOk() throws Exception {
            doNothing().when(userProfileService).unblockUser("test@test.com");

            mockMvc.perform(post("/api/users/unblock/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User test@test.com has been unblocked"));
        }

        @Test
        @DisplayName("POST /api/users/reactivate-request - Should submit reactivation request")
        void reactivateRequest_ShouldReturnOk() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("email", "test@test.com");
            request.put("reason", "False positive");
            request.put("confirmation", "Yes");

            doNothing().when(userProfileService).processReactivationRequest(anyMap());

            mockMvc.perform(post("/api/users/reactivate-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("POST /api/users/send-reactivation-email/{email} - Should send reactivation email")
        void sendReactivationEmail_ShouldReturnOk() throws Exception {
            doNothing().when(userProfileService).sendReactivationEmail("test@test.com");

            mockMvc.perform(post("/api/users/send-reactivation-email/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Reactivation email sent to test@test.com"));
        }
    }

    // ==================== PUT ENDPOINT TESTS ====================

    @Nested
    @DisplayName("PUT Endpoint Tests")
    class PutEndpointTests {

        @Test
        @DisplayName("PUT /api/users/profile/{email} - Should update user profile by email")
        void updateUserProfile_ShouldReturnUpdatedUser() throws Exception {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setEmail("test@test.com");
            dto.setFirstName("Updated");

            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("Updated");

            when(userProfileService.updateUserProfile(eq("test@test.com"), any(UpdateUserRequest.class)))
                    .thenReturn(dto);

            mockMvc.perform(put("/api/users/profile/test@test.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/users/{id} - Should update user by ID")
        void updateUserById_ShouldReturnOk() throws Exception {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(1L);
            dto.setFirstName("Updated");

            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("Updated");

            when(userProfileService.updateUser(eq(1L), any(UpdateUserRequest.class)))
                    .thenReturn(dto);

            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"));
        }
    }

    // ==================== DELETE ENDPOINT TESTS ====================

    @Nested
    @DisplayName("DELETE Endpoint Tests")
    class DeleteEndpointTests {

        @Test
        @DisplayName("DELETE /api/users/{id} - Should delete user and return 204")
        void deleteUser_ShouldReturnNoContent() throws Exception {
            doNothing().when(userProfileService).deleteUser(1L);

            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isNoContent());

            verify(userProfileService, times(1)).deleteUser(1L);
        }
    }

    // ==================== STATISTICS ENDPOINT TESTS ====================

    @Nested
    @DisplayName("GET Statistics Endpoint Tests")
    class StatisticsEndpointTests {

        @Test
        @DisplayName("GET /api/users/statistics - Should return statistics")
        void getStatistics_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.findAllEventsLast24h(any()))
                    .thenReturn(List.of());
            when(loginHistoryRepository.countByActive(true)).thenReturn(0L);
            when(loginHistoryRepository.countBySuspiciousAndLoginTimeAfter(anyBoolean(), any()))
                    .thenReturn(0L);

            mockMvc.perform(get("/api/users/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.logins").exists())
                    .andExpect(jsonPath("$.logouts").exists())
                    .andExpect(jsonPath("$.activeSessions").exists())
                    .andExpect(jsonPath("$.suspicious").exists());
        }

        @Test
        @DisplayName("GET /api/users/active-sessions - Should return active sessions")
        void getActiveSessions_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.findByActiveTrueOrderByLoginTimeDesc())
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/active-sessions"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/recent-logins - Should return recent logins")
        void getRecentLogins_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.findTop20ByOrderByLoginTimeDesc())
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/recent-logins"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/logins/today - Should return today's logins")
        void getTodayLogins_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.findByLoginTimeAfterOrderByLoginTimeDesc(any()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/logins/today"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/sessions/active-count - Should return active session count")
        void getActiveSessionCount_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.countByActive(true)).thenReturn(5L);

            mockMvc.perform(get("/api/users/sessions/active-count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }

        @Test
        @DisplayName("GET /api/users/logins/suspicious-count - Should return suspicious count")
        void getSuspiciousCount_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.countBySuspiciousAndLoginTimeAfter(anyBoolean(), any()))
                    .thenReturn(2L);

            mockMvc.perform(get("/api/users/logins/suspicious-count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("2"));
        }

        @Test
        @DisplayName("GET /api/users/recent-logins-formatted - Should return formatted messages")
        void getRecentLoginsFormatted_ShouldReturnOk() throws Exception {
            when(loginHistoryRepository.findAllEventsLast24h(any()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/recent-logins-formatted"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== REACTIVATION ENDPOINT TESTS ====================

    @Nested
    @DisplayName("Reactivation Endpoint Tests")
    class ReactivationEndpointTests {

        @Test
        @DisplayName("GET /api/users/reactivate/{token} - Should validate token")
        void validateReactivationToken_ShouldReturnValidation() throws Exception {
            when(userProfileService.validateReactivationToken("valid-token")).thenReturn(true);

            mockMvc.perform(get("/api/users/reactivate/valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("GET /api/users/reactivate/{token} - Should return false for invalid token")
        void validateReactivationToken_InvalidToken_ShouldReturnFalse() throws Exception {
            when(userProfileService.validateReactivationToken("invalid-token")).thenReturn(false);

            mockMvc.perform(get("/api/users/reactivate/invalid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        @DisplayName("GET /api/users/unblock/{email} - Should unblock user via GET")
        void unblockUserGet_ShouldReturnOk() throws Exception {
            doNothing().when(userProfileService).unblockUser("test@test.com");

            mockMvc.perform(get("/api/users/unblock/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User test@test.com has been unblocked. You can now login."));
        }

        @Test
        @DisplayName("GET /api/users/check-blocked/{email} - Should return block status")
        void checkBlocked_ShouldReturnStatus() throws Exception {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setBlocked(false);
            dto.setBlockedReason(null);

            when(userProfileService.getUserByEmail("test@test.com")).thenReturn(dto);

            mockMvc.perform(get("/api/users/check-blocked/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blocked").value(false))
                    .andExpect(jsonPath("$.blockedReason").doesNotExist());
        }

        @Test
        @DisplayName("GET /api/users/check-blocked/{email} - Should return true for blocked user")
        void checkBlocked_BlockedUser_ShouldReturnTrue() throws Exception {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setBlocked(true);
            dto.setBlockedReason("Suspicious activity");

            when(userProfileService.getUserByEmail("test@test.com")).thenReturn(dto);

            mockMvc.perform(get("/api/users/check-blocked/test@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blocked").value(true))
                    .andExpect(jsonPath("$.blockedReason").value("Suspicious activity"));
        }
    }


}