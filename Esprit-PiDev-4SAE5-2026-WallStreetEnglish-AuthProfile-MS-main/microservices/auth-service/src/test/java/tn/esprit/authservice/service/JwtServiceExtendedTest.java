package tn.esprit.authservice.service;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Extended Tests")
class JwtServiceExtendedTest {

    @InjectMocks
    private JwtService jwtService;

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        userDetails = User.builder()
                .username("user@test.com")
                .password("")
                .authorities(() -> "ROLE_STUDENT")
                .build();
    }

    @Nested
    @DisplayName("Token generation")
    class GenerationTests {

        @Test
        @DisplayName("Generated token has three parts separated by dots")
        void generateToken_HasThreeParts() {
            String token = jwtService.generateToken(userDetails);
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("Two calls with same user produce different tokens (timestamp differs)")
        void generateToken_TwoCalls_ProduceDifferentTokens() throws InterruptedException {
            String t1 = jwtService.generateToken(userDetails);
            Thread.sleep(10);
            String t2 = jwtService.generateToken(userDetails);
            // Both valid but issued at different times
            assertEquals("user@test.com", jwtService.extractUsername(t1));
            assertEquals("user@test.com", jwtService.extractUsername(t2));
        }

        @Test
        @DisplayName("Token with no extra claims has null role and userId")
        void generateToken_NoExtraClaims_NullRoleAndUserId() {
            String token = jwtService.generateToken(userDetails);
            assertNull(jwtService.extractRole(token));
            assertNull(jwtService.extractUserId(token));
        }
    }

    @Nested
    @DisplayName("Token validation")
    class ValidationTests {

        @Test
        @DisplayName("Expired token should throw exception during validation")
        void isTokenValid_ExpiredToken_ThrowsExpiredJwtException() {
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
            String expiredToken = jwtService.generateToken(userDetails);
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

            assertThrows(ExpiredJwtException.class, () ->
                    jwtService.isTokenValid(expiredToken, userDetails));
        }

        @Test
        @DisplayName("Token is invalid for user with different username")
        void isTokenValid_WrongUser_ReturnsFalse() {
            String token = jwtService.generateToken(userDetails);
            UserDetails other = User.builder()
                    .username("other@test.com")
                    .password("")
                    .authorities(() -> "ROLE_STUDENT")
                    .build();
            assertFalse(jwtService.isTokenValid(token, other));
        }
    }

    @Nested
    @DisplayName("Claim extraction")
    class ClaimExtractionTests {

        @Test
        @DisplayName("Should extract correct email from token subject")
        void extractUsername_ReturnsEmail() {
            String token = jwtService.generateToken(userDetails);
            assertEquals("user@test.com", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("Should extract role from extra claims")
        void extractRole_ReturnsRole() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "TUTOR");
            String token = jwtService.generateToken(claims, userDetails);
            assertEquals("TUTOR", jwtService.extractRole(token));
        }

        @Test
        @DisplayName("Should extract userId as Long from extra claims")
        void extractUserId_ReturnsLong() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", 42L);
            String token = jwtService.generateToken(claims, userDetails);
            assertEquals(42L, jwtService.extractUserId(token));
        }

        @Test
        @DisplayName("Should extract all claims for a rich token")
        void generateToken_AllExtraClaims_AllExtractable() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "ADMIN");
            claims.put("userId", 99L);
            String token = jwtService.generateToken(claims, userDetails);

            assertEquals("user@test.com", jwtService.extractUsername(token));
            assertEquals("ADMIN", jwtService.extractRole(token));
            assertEquals(99L, jwtService.extractUserId(token));
        }
    }
}