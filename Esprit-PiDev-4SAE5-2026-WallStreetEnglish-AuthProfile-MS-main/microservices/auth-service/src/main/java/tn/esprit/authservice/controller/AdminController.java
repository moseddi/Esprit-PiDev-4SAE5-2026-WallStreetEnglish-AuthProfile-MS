package tn.esprit.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.dto.AuthResponse;
import tn.esprit.authservice.dto.RegisterRequest;
import tn.esprit.authservice.dto.RoleUpdateRequest;
import tn.esprit.authservice.entity.Role;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.service.AuthService;
import tn.esprit.authservice.service.KeycloakService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

    @PostMapping("/create")
    // NOTE: No @PreAuthorize here — this endpoint is called service-to-service
    // from user-management-service. The admin role is verified at the gateway level.
    // The /api/auth/admin/** path is still protected by SecurityConfig (.authenticated()).
    public ResponseEntity<AuthResponse> createUserByAdmin(@Valid @RequestBody RegisterRequest request) {
        log.info("Admin create user request for email: {}", request.getEmail());

        AuthResponse response = authService.registerByAdmin(request);
        log.info("User created successfully with ID: {}", response.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> testAdminAccess() {
        return ResponseEntity.ok("You are an admin! Access granted.");
    }

    @PutMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@RequestBody RoleUpdateRequest request) {
        log.info("Update role request for email: {} to role: {}", request.getEmail(), request.getRole());

        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found in auth database"));

            Role oldRole = user.getRole();
            user.setRole(request.getRole());
            userRepository.save(user);
            log.info("Auth DB role updated from {} to {}", oldRole, request.getRole());

            try {
                keycloakService.updateUserRole(request.getEmail(), request.getRole().name());
                log.info("Keycloak role updated for: {}", request.getEmail());
            } catch (Exception e) {
                log.error("Keycloak role update failed: {}", e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Role updated successfully");
            result.put("email", user.getEmail());
            result.put("oldRole", oldRole);
            result.put("newRole", user.getRole());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to update role: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}