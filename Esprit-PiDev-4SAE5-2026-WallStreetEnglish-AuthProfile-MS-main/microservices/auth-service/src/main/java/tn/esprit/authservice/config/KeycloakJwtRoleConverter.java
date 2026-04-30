package tn.esprit.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
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
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        Set<String> allRoles = new HashSet<>();

        extractRealmRoles(jwt, allRoles);
        extractResourceRoles(jwt, allRoles);
        extractTopLevelRoles(jwt, allRoles);
        fallbackToDatabaseRole(jwt, allRoles);

        if (allRoles.isEmpty()) {
            log.warn("No roles found in token or database for user: {}", jwt.getClaimAsString("preferred_username"));
        } else {
            log.debug("Final authorities for {}: {}", jwt.getClaimAsString(CLAIM_EMAIL), allRoles);
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : allRoles) {
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

    private void fallbackToDatabaseRole(Jwt jwt, Set<String> allRoles) {
        boolean hasBusinessRole = allRoles.stream()
                .anyMatch(r -> r.equals("ADMIN") || r.equals("STUDENT") || r.equals("TUTOR"));

        if (!hasBusinessRole) {
            String email = jwt.getClaimAsString(CLAIM_EMAIL);
            if (email != null) {
                userRepository.findByEmail(email).ifPresent(user -> {
                    allRoles.add(user.getRole().name());
                    log.debug("Fallback: found role in database for {}: {}", email, user.getRole());
                });
            }
        }
    }
}
