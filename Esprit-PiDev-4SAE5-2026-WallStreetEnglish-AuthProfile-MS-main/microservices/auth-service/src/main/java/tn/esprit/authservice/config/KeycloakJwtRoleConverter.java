package tn.esprit.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.authservice.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    private final UserRepository userRepository;

    @Override
    @NonNull
    @Transactional
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        Set<String> allRoles = new HashSet<>();

        // 1. Extract raw roles from various layers of the Keycloak token
        extractRealmRoles(jwt, allRoles);
        extractResourceRoles(jwt, allRoles);
        extractTopLevelRoles(jwt, allRoles);

        // Standardize strings: strip "ROLE_" prefix if present and convert to uppercase
        Set<String> cleanedRoles = new HashSet<>();
        for (String r : allRoles) {
            if (r != null) {
                cleanedRoles.add(r.toUpperCase().replace("ROLE_", ""));
            }
        }

        // 2. Locate the primary business role coming from the Keycloak token
        String tokenBusinessRole = cleanedRoles.stream()
                .filter(r -> r.equals("ADMIN") || r.equals("STUDENT") || r.equals("TUTOR"))
                .findFirst()
                .orElse(null);

        String email = jwt.getClaimAsString(CLAIM_EMAIL);

        if (tokenBusinessRole != null && email != null) {
            // Keycloak has a concrete business role. Verify and sync with local DB if mismatched.
            userRepository.findByEmail(email).ifPresent(user -> {
                if (!user.getRole().name().equalsIgnoreCase(tokenBusinessRole)) {
                    log.info("🔄 Mismatch detected! Keycloak token says '{}' but Local DB says '{}'. Synchronizing Local DB for: {}",
                            tokenBusinessRole, user.getRole().name(), email);
                    try {
                        user.setRole(tn.esprit.authservice.entity.Role.valueOf(tokenBusinessRole));
                        userRepository.save(user);
                    } catch (Exception e) {
                        log.error("❌ Failed to sync token role to database for user {}", email, e);
                    }
                }
            });
        } else if (tokenBusinessRole == null && email != null) {
            // Fallback to local database role only if Keycloak token completely lacks a business role
            userRepository.findByEmail(email).ifPresent(user -> {
                cleanedRoles.add(user.getRole().name());
                log.debug("Fallback: No business role in token. Loaded database role for {}: {}", email, user.getRole());
            });
        }

        if (cleanedRoles.isEmpty()) {
            log.warn("No roles found in token or database for user: {}", jwt.getClaimAsString("preferred_username"));
        } else {
            log.info("Final evaluated roles for {}: {}", email, cleanedRoles);
        }

        // 3. Map final cleaned strings into proper Spring GrantedAuthorities
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : cleanedRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    private void extractRealmRoles(Jwt jwt, Set<String> allRoles) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey(CLAIM_ROLES)) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get(CLAIM_ROLES);
            if (roles != null) {
                allRoles.addAll(roles);
            }
        }
    }

    private void extractResourceRoles(Jwt jwt, Set<String> allRoles) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) return;

        resourceAccess.values().forEach(client -> {
            if (client instanceof Map<?, ?> clientMap && clientMap.containsKey(CLAIM_ROLES)) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) clientMap.get(CLAIM_ROLES);
                if (roles != null) {
                    allRoles.addAll(roles);
                }
            }
        });
    }

    private void extractTopLevelRoles(Jwt jwt, Set<String> allRoles) {
        if (!jwt.hasClaim(CLAIM_ROLES)) return;

        Object rolesObj = jwt.getClaim(CLAIM_ROLES);
        if (rolesObj instanceof List<?> roleList) {
            roleList.forEach(r -> {
                if (r instanceof String s) allRoles.add(s);
            });
        } else if (rolesObj instanceof String s) {
            allRoles.add(s);
        }
    }
}