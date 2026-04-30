package tn.esprit.usermanagementservice.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url:http://keycloak:8080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:myapp2}")
    private String realm;

    private static final String ADMIN_REALMS_PATH = "/admin/realms/";

    public void logoutUserSessions(String email) {
        try {
            String token = getAdminToken();
            if (token == null) {
                log.error("Cannot logout: Failed to get admin token");
                return;
            }

            String userId = getUserId(email, token);
            if (userId == null) {
                log.error("Cannot logout: User not found in Keycloak");
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> request = new HttpEntity<>(headers);

            String url = keycloakUrl + ADMIN_REALMS_PATH + realm + "/users/" + userId + "/logout";
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            log.info("Logged out user: {}", email);
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
        }
    }

    public void updateUserRole(String email, String newRole) {
        try {
            log.info("Updating role for email={} to role={}", email, newRole);

            String token = getAdminToken();
            if (token == null) {
                log.error("Failed to get admin token - cannot update role");
                return;
            }

            String userId = getUserId(email, token);
            if (userId == null) {
                log.error("User not found in Keycloak: {}", email);
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String realmRolesUrl = keycloakUrl + ADMIN_REALMS_PATH + realm + "/roles";
            HttpEntity<?> getRolesRequest = new HttpEntity<>(headers);
            ResponseEntity<KeycloakRole[]> rolesResponse = restTemplate.exchange(
                    realmRolesUrl, HttpMethod.GET, getRolesRequest, KeycloakRole[].class);

            if (rolesResponse.getBody() == null) {
                log.error("No roles found in Keycloak");
                return;
            }

            KeycloakRole targetRole = Arrays.stream(rolesResponse.getBody())
                    .filter(role -> role.getName().equals(newRole))
                    .findFirst()
                    .orElse(null);

            if (targetRole == null) {
                log.error("Role {} not found in Keycloak", newRole);
                return;
            }

            String userRolesUrl = keycloakUrl + ADMIN_REALMS_PATH + realm + "/users/" + userId + "/role-mappings/realm";

            HttpEntity<?> getUserRolesRequest = new HttpEntity<>(headers);
            ResponseEntity<KeycloakRole[]> userRolesResponse = restTemplate.exchange(
                    userRolesUrl, HttpMethod.GET, getUserRolesRequest, KeycloakRole[].class);

            if (userRolesResponse.getBody() != null && userRolesResponse.getBody().length > 0) {
                KeycloakRole[] rolesToRemove = Arrays.stream(userRolesResponse.getBody())
                        .filter(role -> !role.getName().equals("default-roles-myapp2")
                                && !role.getName().equals("offline_access")
                                && !role.getName().equals("uma_authorization"))
                        .toArray(KeycloakRole[]::new);

                if (rolesToRemove.length > 0) {
                    HttpEntity<KeycloakRole[]> deleteRequest = new HttpEntity<>(rolesToRemove, headers);
                    restTemplate.exchange(userRolesUrl, HttpMethod.DELETE, deleteRequest, String.class);
                    log.info("Removed {} existing roles for user: {}", rolesToRemove.length, email);
                }
            }

            KeycloakRole[] rolesToAdd = new KeycloakRole[]{targetRole};
            HttpEntity<KeycloakRole[]> addRequest = new HttpEntity<>(rolesToAdd, headers);
            restTemplate.exchange(userRolesUrl, HttpMethod.POST, addRequest, String.class);

            log.info("Successfully updated role to {} for user: {}", newRole, email);
            logoutUserSessions(email);

        } catch (Exception e) {
            log.error("Failed to update role in Keycloak: {}", e.getMessage(), e);
        }
    }

    private String getAdminToken() {
        String url = keycloakUrl + "/realms/master/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=admin-cli&username=admin&password=admin&grant_type=password";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(url, request, KeycloakTokenResponse.class);

            if (response.getBody() != null && response.getBody().getAccessToken() != null) {
                return response.getBody().getAccessToken();
            } else {
                log.error("Failed to get admin token - empty response. Status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to get admin token: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getUserId(String email, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> request = new HttpEntity<>(headers);

        String url = keycloakUrl + ADMIN_REALMS_PATH + realm + "/users?email=" + email;

        try {
            ResponseEntity<KeycloakUser[]> response = restTemplate.exchange(url, HttpMethod.GET, request, KeycloakUser[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                return response.getBody()[0].getId();
            }
        } catch (Exception e) {
            log.error("Failed to get user ID for {}: {}", email, e.getMessage());
        }

        log.error("User not found in Keycloak with email: {}", email);
        return null;
    }

    // ── Inner DTO classes ────────────────────────────────────────────────────

    private static class KeycloakTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    private static class KeycloakUser {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    private static class KeycloakRole {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}