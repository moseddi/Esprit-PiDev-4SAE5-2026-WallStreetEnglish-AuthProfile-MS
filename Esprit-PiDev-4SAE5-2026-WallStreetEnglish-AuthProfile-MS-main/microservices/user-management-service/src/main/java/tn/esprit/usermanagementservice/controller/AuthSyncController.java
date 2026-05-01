package tn.esprit.usermanagementservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.usermanagementservice.entity.Role;
import tn.esprit.usermanagementservice.service.UserProfileService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class AuthSyncController {

    private final UserProfileService userProfileService;

    /**
     * Endpoint for auth service to create minimal profile when user registers.
     * This is called by the auth service after Keycloak registration.
     */
    @PostMapping("/from-auth")
    public ResponseEntity<String> createProfileFromAuth(@RequestBody Map<String, String> authData) {
        String email = authData.get("email");
        String role = authData.get("role");

        log.info("Auth sync: creating minimal profile for email={}, role={}", email, role);

        userProfileService.createMinimalProfile(email, Role.valueOf(role));

        return ResponseEntity.ok("Profile created successfully");
    }
}