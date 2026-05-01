package tn.esprit.usermanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.usermanagementservice.dto.CreateUserRequest;
import tn.esprit.usermanagementservice.dto.UpdateUserRequest;
import tn.esprit.usermanagementservice.dto.UserProfileDTO;
import tn.esprit.usermanagementservice.entity.LoginHistory;
import tn.esprit.usermanagementservice.entity.Role;
import tn.esprit.usermanagementservice.entity.UserProfile;
import tn.esprit.usermanagementservice.repository.LoginHistoryRepository;
import tn.esprit.usermanagementservice.repository.UserProfileRepository;
import tn.esprit.usermanagementservice.service.KeycloakAdminService;
import tn.esprit.usermanagementservice.service.UserProfileService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Hamcrest — explicit to avoid clash with Mockito ArgumentMatchers
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;

// Mockito — explicit to avoid clash with Hamcrest Matchers
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserProfileService       userProfileService;
    @MockitoBean KeycloakAdminService     keycloakAdminService;
    @MockitoBean LoginHistoryRepository   loginHistoryRepository;
    @MockitoBean UserProfileRepository    userProfileRepository;

    private UserProfileDTO sampleDTO;
    private UserProfile    sampleUser;

    @BeforeEach
    void setUp() {
        sampleDTO = new UserProfileDTO();
        sampleDTO.setId(1L);
        sampleDTO.setEmail("student@test.com");
        sampleDTO.setFirstName("John");
        sampleDTO.setLastName("Doe");
        sampleDTO.setRole(Role.STUDENT);
        sampleDTO.setActive(true);
        sampleDTO.setBlocked(false);

        sampleUser = new UserProfile();
        sampleUser.setId(1L);
        sampleUser.setEmail("student@test.com");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setRole(Role.STUDENT);
        sampleUser.setActive(true);
        sampleUser.setBlocked(false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/users  — createUser
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class CreateUserTests {

        @Test
        void createUser_returnsCreatedDTO() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("student@test.com");
            req.setFirstName("John");
            req.setLastName("Doe");
            req.setRole(Role.STUDENT);

            when(userProfileService.createUser(any(CreateUserRequest.class), any()))
                    .thenReturn(sampleDTO);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("student@test.com"))
                    .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        void createUser_withoutAuthHeader_returnsCreatedDTO() throws Exception {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("student@test.com");
            req.setFirstName("John");
            req.setLastName("Doe");
            req.setRole(Role.STUDENT);

            when(userProfileService.createUser(any(CreateUserRequest.class), isNull()))
                    .thenReturn(sampleDTO);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("student@test.com"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT /api/users/profile/{email} — completeProfile
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class CompleteProfileTests {

        @Test
        void completeProfile_returnsUpdatedDTO() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setFirstName("Jane");

            when(userProfileService.updateUserProfile(eq("student@test.com"), any()))
                    .thenReturn(sampleDTO);

            mockMvc.perform(put("/api/users/profile/student@test.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("student@test.com"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users/{id} — getUserById
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class GetUserByIdTests {

        @Test
        void getUserById_returnsDTO() throws Exception {
            when(userProfileService.getUserById(1L)).thenReturn(sampleDTO);

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        void getUserById_notFound_propagatesException() {
            when(userProfileService.getUserById(99L))
                    .thenThrow(new RuntimeException("User not found"));

            // No @ExceptionHandler in controller — Spring wraps RuntimeException
            // in a ServletException; assert it throws instead of checking HTTP status.
            org.junit.jupiter.api.Assertions.assertThrows(
                    jakarta.servlet.ServletException.class,
                    () -> mockMvc.perform(get("/api/users/99"))
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users/email/{email} — getUserByEmail
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class GetUserByEmailTests {

        @Test
        void getUserByEmail_returnsDTO() throws Exception {
            when(userProfileService.getUserByEmail("student@test.com")).thenReturn(sampleDTO);

            mockMvc.perform(get("/api/users/email/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("student@test.com"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users — getAllUsers
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class GetAllUsersTests {

        @Test
        void getAllUsers_returnsList() throws Exception {
            when(userProfileService.getAllUsers()).thenReturn(List.of(sampleDTO));

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].email").value("student@test.com"));
        }

        @Test
        void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
            when(userProfileService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users/role/{role} — getUsersByRole
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class GetUsersByRoleTests {

        @Test
        void getUsersByRole_student_returnsList() throws Exception {
            when(userProfileService.getUsersByRole(Role.STUDENT)).thenReturn(List.of(sampleDTO));

            mockMvc.perform(get("/api/users/role/STUDENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].role").value("STUDENT"));
        }

        @Test
        void getUsersByRole_tutor_returnsEmptyList() throws Exception {
            when(userProfileService.getUsersByRole(Role.TUTOR)).thenReturn(List.of());

            mockMvc.perform(get("/api/users/role/TUTOR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT /api/users/{id} — updateUser
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class UpdateUserTests {

        @Test
        void updateUser_returnsUpdatedDTO() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setFirstName("Updated");

            when(userProfileService.updateUser(eq(1L), any())).thenReturn(sampleDTO);

            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE /api/users/{id} — deleteUser
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class DeleteUserTests {

        @Test
        void deleteUser_returns204() throws Exception {
            doNothing().when(userProfileService).deleteUser(1L);

            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isNoContent());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users/test
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class TestEndpointTests {

        @Test
        void testEndpoint_returnsWorkingMessage() throws Exception {
            mockMvc.perform(get("/api/users/test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User Management Service is working!"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/users/record-login  &  /record-logout
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class LoginLogoutRecordTests {

        @Test
        void recordLogin_returns200() throws Exception {
            doNothing().when(userProfileService).recordUserLogin("student@test.com");

            mockMvc.perform(post("/api/users/record-login")
                            .param("email", "student@test.com"))
                    .andExpect(status().isOk());
        }

        @Test
        void recordLogout_voluntary_returns200() throws Exception {
            doNothing().when(userProfileService)
                    .recordUserLogout("student@test.com", LoginHistory.LogoutType.VOLUNTARY);

            mockMvc.perform(post("/api/users/record-logout")
                            .param("email", "student@test.com")
                            .param("logoutType", "VOLUNTARY"))
                    .andExpect(status().isOk());
        }

        @Test
        void recordLogout_defaultLogoutType_voluntary() throws Exception {
            // No logoutType param → default VOLUNTARY
            doNothing().when(userProfileService)
                    .recordUserLogout(eq("student@test.com"), any());

            mockMvc.perform(post("/api/users/record-logout")
                            .param("email", "student@test.com"))
                    .andExpect(status().isOk());
        }

        @Test
        void recordLogout_forced_returns200() throws Exception {
            doNothing().when(userProfileService)
                    .recordUserLogout("student@test.com", LoginHistory.LogoutType.FORCED);

            mockMvc.perform(post("/api/users/record-logout")
                            .param("email", "student@test.com")
                            .param("logoutType", "FORCED"))
                    .andExpect(status().isOk());
        }

        @Test
        void recordLogout_timeout_returns200() throws Exception {
            doNothing().when(userProfileService)
                    .recordUserLogout("student@test.com", LoginHistory.LogoutType.TIMEOUT);

            mockMvc.perform(post("/api/users/record-logout")
                            .param("email", "student@test.com")
                            .param("logoutType", "TIMEOUT"))
                    .andExpect(status().isOk());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/users/force-logout/{email}
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class ForceLogoutTests {

        @Test
        void forceLogout_callsKeycloakAndRecords() throws Exception {
            doNothing().when(keycloakAdminService).logoutUserSessions("student@test.com");
            doNothing().when(userProfileService)
                    .recordUserLogout("student@test.com", LoginHistory.LogoutType.FORCED);

            mockMvc.perform(post("/api/users/force-logout/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("student@test.com")));

            verify(keycloakAdminService).logoutUserSessions("student@test.com");
            verify(userProfileService).recordUserLogout("student@test.com", LoginHistory.LogoutType.FORCED);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Security monitor endpoints
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class SecurityMonitorTests {

        @Test
        void getRecentLogins_returnsList() throws Exception {
            LoginHistory lh = new LoginHistory();
            lh.setId(1L);
            lh.setEmail("student@test.com");
            lh.setType(LoginHistory.EventType.LOGIN);
            lh.setLoginTime(LocalDateTime.now());
            lh.setActive(true);

            when(loginHistoryRepository.findTop20ByOrderByLoginTimeDesc())
                    .thenReturn(List.of(lh));

            mockMvc.perform(get("/api/users/recent-logins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void getTodayLogins_returnsList() throws Exception {
            when(loginHistoryRepository.findByLoginTimeAfterOrderByLoginTimeDesc(any()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/logins/today"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void getActiveSessionCount_returnsCount() throws Exception {
            when(loginHistoryRepository.countByActive(true)).thenReturn(5L);

            mockMvc.perform(get("/api/users/sessions/active-count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }

        @Test
        void getSuspiciousCount_returnsCount() throws Exception {
            when(loginHistoryRepository.countBySuspiciousAndLoginTimeAfter(eq(true), any()))
                    .thenReturn(3L);

            mockMvc.perform(get("/api/users/logins/suspicious-count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
        }

        @Test
        void getActiveSessions_returnsList() throws Exception {
            when(loginHistoryRepository.findByActiveTrueOrderByLoginTimeDesc())
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/active-sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/users/sync-from-auth
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class SyncFromAuthTests {

        @Test
        void syncFromAuth_returnsDTO() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "student@test.com");
            payload.put("role", "STUDENT");

            when(userProfileService.syncUserFromAuth("student@test.com", "STUDENT"))
                    .thenReturn(sampleDTO);

            mockMvc.perform(post("/api/users/sync-from-auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("student@test.com"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users/statistics
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class StatisticsTests {

        @Test
        void getStatistics_mixedEvents_returnsCorrectCounts() throws Exception {
            LoginHistory login = new LoginHistory();
            login.setType(LoginHistory.EventType.LOGIN);
            login.setLoginTime(LocalDateTime.now());

            LoginHistory logout = new LoginHistory();
            logout.setType(LoginHistory.EventType.LOGOUT);
            logout.setLoginTime(LocalDateTime.now());

            when(loginHistoryRepository.findAllEventsLast24h(any()))
                    .thenReturn(List.of(login, logout));
            when(loginHistoryRepository.countByActive(true)).thenReturn(2L);
            when(loginHistoryRepository.countBySuspiciousAndLoginTimeAfter(eq(true), any()))
                    .thenReturn(1L);

            mockMvc.perform(get("/api/users/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.logins").value(1))
                    .andExpect(jsonPath("$.logouts").value(1))
                    .andExpect(jsonPath("$.activeSessions").value(2))
                    .andExpect(jsonPath("$.suspicious").value(1))
                    .andExpect(jsonPath("$.period").value("Last 24 Hours"));
        }

        @Test
        void getStatistics_noEvents_returnsZeros() throws Exception {
            when(loginHistoryRepository.findAllEventsLast24h(any())).thenReturn(List.of());
            when(loginHistoryRepository.countByActive(true)).thenReturn(0L);
            when(loginHistoryRepository.countBySuspiciousAndLoginTimeAfter(eq(true), any()))
                    .thenReturn(0L);

            mockMvc.perform(get("/api/users/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.logins").value(0))
                    .andExpect(jsonPath("$.logouts").value(0));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/users/recent-logins-formatted
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class RecentLoginsFormattedTests {

        @Test
        void getRecentLoginsFormatted_returnsMessages() throws Exception {
            LoginHistory lh = new LoginHistory();
            lh.setType(LoginHistory.EventType.LOGIN);
            lh.setLoginTime(LocalDateTime.now());
            lh.setEmail("student@test.com");

            when(loginHistoryRepository.findAllEventsLast24h(any())).thenReturn(List.of(lh));

            mockMvc.perform(get("/api/users/recent-logins-formatted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void getRecentLoginsFormatted_emptyEvents_returnsEmptyList() throws Exception {
            when(loginHistoryRepository.findAllEventsLast24h(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/users/recent-logins-formatted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Block / Unblock endpoints
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class BlockUnblockTests {

        @Test
        void blockUser_returns200WithMessage() throws Exception {
            doNothing().when(userProfileService).blockUser("student@test.com", "Suspicious activity");

            mockMvc.perform(post("/api/users/block/student@test.com")
                            .param("reason", "Suspicious activity"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("blocked")));

            verify(userProfileService).blockUser("student@test.com", "Suspicious activity");
        }

        @Test
        void unblockUser_post_returns200WithMessage() throws Exception {
            doNothing().when(userProfileService).unblockUser("student@test.com");

            mockMvc.perform(post("/api/users/unblock/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("unblocked")));

            verify(userProfileService).unblockUser("student@test.com");
        }

        @Test
        void unblockUser_get_returns200WithMessage() throws Exception {
            doNothing().when(userProfileService).unblockUser("student@test.com");

            mockMvc.perform(get("/api/users/unblock/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("unblocked")));

            verify(userProfileService).unblockUser("student@test.com");
        }

        @Test
        void checkBlocked_notBlocked_returnsFalse() throws Exception {
            when(userProfileService.getUserByEmail("student@test.com")).thenReturn(sampleDTO);

            mockMvc.perform(get("/api/users/check-blocked/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blocked").value(false))
                    .andExpect(jsonPath("$.blockedReason").doesNotExist());
        }

        @Test
        void checkBlocked_blocked_returnsReason() throws Exception {
            UserProfileDTO blocked = new UserProfileDTO();
            blocked.setId(1L);
            blocked.setEmail("student@test.com");
            blocked.setBlocked(true);
            blocked.setBlockedReason("Suspicious activity");
            when(userProfileService.getUserByEmail("student@test.com")).thenReturn(blocked);

            mockMvc.perform(get("/api/users/check-blocked/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blocked").value(true))
                    .andExpect(jsonPath("$.blockedReason").value("Suspicious activity"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Reactivation endpoints
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    class ReactivationTests {

        @Test
        void validateReactivationToken_validToken_returnsValidTrueAndEmail() throws Exception {
            when(userProfileService.validateReactivationToken("abc123")).thenReturn(true);
            when(userProfileRepository.findAll()).thenReturn(List.of(sampleUser));
            sampleUser.setReactivationToken("abc123");

            mockMvc.perform(get("/api/users/reactivate/abc123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.email").value("student@test.com"));
        }

        @Test
        void validateReactivationToken_invalidToken_returnsValidFalse() throws Exception {
            when(userProfileService.validateReactivationToken("bad-token")).thenReturn(false);

            mockMvc.perform(get("/api/users/reactivate/bad-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        void validateReactivationToken_validButNoMatchingUser_noEmailInResponse() throws Exception {
            when(userProfileService.validateReactivationToken("orphan-token")).thenReturn(true);
            // All users have different tokens
            when(userProfileRepository.findAll()).thenReturn(List.of(sampleUser));
            sampleUser.setReactivationToken("different-token");

            mockMvc.perform(get("/api/users/reactivate/orphan-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        void requestReactivation_returnsSuccessMessage() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("email", "student@test.com");

            doNothing().when(userProfileService).processReactivationRequest(any());

            mockMvc.perform(post("/api/users/reactivate-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Reactivation request submitted successfully"));
        }

        @Test
        void sendReactivationEmail_returns200() throws Exception {
            doNothing().when(userProfileService).sendReactivationEmail("student@test.com");

            mockMvc.perform(post("/api/users/send-reactivation-email/student@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("student@test.com")));

            verify(userProfileService).sendReactivationEmail("student@test.com");
        }
    }
}