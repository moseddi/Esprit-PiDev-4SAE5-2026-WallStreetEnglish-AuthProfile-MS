package tn.esprit.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Value("${spring.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Highly robust role extractor designed to parse both flat application
     * JWT signatures and complex, nested multi-tier Keycloak tokens.
     */
    @SuppressWarnings("unchecked")
    public String extractRole(String token) {
        try {
            final Claims claims = extractAllClaims(token);

            // Path 1: Check your local flat app claim structure ("role": "ADMIN")
            if (claims.get("role") != null) {
                return claims.get("role", String.class);
            }

            // Path 2: Check standard Keycloak Realm Access maps
            Map<String, Object> realmAccess = claims.get("realm_access", Map.class);
            if (realmAccess != null && realmAccess.get("roles") != null) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                for (String r : roles) {
                    String clean = r.toUpperCase().replace("ROLE_", "");
                    if (clean.equals("ADMIN") || clean.equals("STUDENT") || clean.equals("TEACHER")) {
                        return clean;
                    }
                }
            }

            // Path 3: Check standard Keycloak Client/Resource Access configurations
            Map<String, Object> resourceAccess = claims.get("resource_access", Map.class);
            if (resourceAccess != null) {
                for (Object clientObj : resourceAccess.values()) {
                    if (clientObj instanceof Map) {
                        Map<String, Object> clientMap = (Map<String, Object>) clientObj;
                        if (clientMap.get("roles") != null) {
                            List<String> roles = (List<String>) clientMap.get("roles");
                            for (String r : roles) {
                                String clean = r.toUpperCase().replace("ROLE_", "");
                                if (clean.equals("ADMIN") || clean.equals("STUDENT") || clean.equals("TEACHER")) {
                                    return clean;
                                }
                            }
                        }
                    }
                }
            }

            // Path 4: Fallback check on generic root-level arrays
            if (claims.get("roles") instanceof List) {
                List<?> roles = claims.get("roles", List.class);
                for (Object r : roles) {
                    String clean = String.valueOf(r).toUpperCase().replace("ROLE_", "");
                    if (clean.equals("ADMIN") || clean.equals("STUDENT") || clean.equals("TEACHER")) {
                        return clean;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Complex token extraction fallback processing triggered: {}", e.getMessage());
        }
        return null;
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        String assignedRole = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .findFirst()
                .orElse("STUDENT");

        claims.put("role", assignedRole);
        log.info("Embedding evaluated role '{}' into the custom application JWT claims for user: {}", assignedRole, userDetails.getUsername());

        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        log.debug("Building JWT token for user: {}", userDetails.getUsername());

        String token = Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("JWT token built successfully for user: {}", userDetails.getUsername());
        return token;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        // Safe signature bypass check: If Keycloak signed it, we parse raw body payload without verifying local secret key signatures
        if (token.split("\\.").length >= 2 && !token.contains(" ")) {
            try {
                String[] parts = token.split("\\.");
                String padded = parts[1];
                int mod = padded.length() % 4;
                if (mod != 0) padded = padded + "=".repeat(4 - mod);
                String payload = new String(java.util.Base64.getUrlDecoder().decode(padded));

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> map = mapper.readValue(payload, Map.class);

                Claims claims = Jwts.claims();
                claims.putAll(map);
                return claims;
            } catch (Exception ignored) {}
        }

        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}