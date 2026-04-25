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
import tn.esprit.usermanagementservice.dto.UpdateUserRequest;
import tn.esprit.usermanagementservice.dto.UserProfileDTO;
import tn.esprit.usermanagementservice.entity.Role;
import tn.esprit.usermanagementservice.repository.LoginHistoryRepository;
import tn.esprit.usermanagementservice.repository.UserProfileRepository;
import tn.esprit.usermanagementservice.service.KeycloakAdminService;
import tn.esprit.usermanagementservice.service.UserProfileService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

    @Nested
    @DisplayName("GET Endpoints Tests")
    class GetEndpointTests {

        @Test
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
        void getAllUsers_ShouldReturnList() throws Exception {
            when(userProfileService.getAllUsers()).thenReturn(java.util.List.of());

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk());
        }

        @Test
        void getUsersByRole_ShouldReturnFilteredList() throws Exception {
            when(userProfileService.getUsersByRole(Role.STUDENT)).thenReturn(java.util.List.of());

            mockMvc.perform(get("/api/users/role/STUDENT"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT Endpoints Tests")
    class PutEndpointTests {

        @Test
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
    }

    @Nested
    @DisplayName("Login/Logout Endpoints Tests")
    class LoginLogoutTests {

        @Test
        void recordUserLogin_ShouldReturnOk() throws Exception {
            mockMvc.perform(post("/api/users/record-login")
                            .param("email", "test@test.com"))
                    .andExpect(status().isOk());
        }

        @Test
        void recordUserLogout_ShouldReturnOk() throws Exception {
            mockMvc.perform(post("/api/users/record-logout")
                            .param("email", "test@test.com")
                            .param("logoutType", "VOLUNTARY"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Block/Unblock Endpoints Tests")
    class BlockUnblockTests {

        @Test
        void blockUser_ShouldReturnOk() throws Exception {
            mockMvc.perform(post("/api/users/block/test@test.com")
                            .param("reason", "Suspicious activity"))
                    .andExpect(status().isOk());
        }

        @Test
        void unblockUser_ShouldReturnOk() throws Exception {
            mockMvc.perform(post("/api/users/unblock/test@test.com"))
                    .andExpect(status().isOk());
        }
    }
}