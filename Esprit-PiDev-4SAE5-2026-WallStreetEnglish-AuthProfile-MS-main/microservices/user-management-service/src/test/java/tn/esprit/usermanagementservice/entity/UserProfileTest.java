package tn.esprit.usermanagementservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserProfile Entity Tests")
class UserProfileTest {

    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userProfile = UserProfile.builder()
                .id(1L)
                .email("test@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.STUDENT)
                .active(true)
                .blocked(false)
                .loginCount(5)
                .build();
    }

    @Test
    @DisplayName("Should create UserProfile with builder")
    void builder_ShouldCreateEntity() {
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getId()).isEqualTo(1L);
        assertThat(userProfile.getEmail()).isEqualTo("test@test.com");
        assertThat(userProfile.getFirstName()).isEqualTo("John");
        assertThat(userProfile.getRole()).isEqualTo(Role.STUDENT);
        assertThat(userProfile.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should set timestamps on prePersist")
    void onCreate_ShouldSetTimestamps() {
        UserProfile newUser = new UserProfile();
        newUser.onCreate();

        assertThat(newUser.getCreatedAt()).isNotNull();
        assertThat(newUser.getUpdatedAt()).isNotNull();
        assertThat(newUser.getAccountCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not override existing accountCreatedAt")
    void onCreate_ShouldNotOverrideAccountCreatedAt() {
        LocalDateTime existingTime = LocalDateTime.now().minusDays(10);
        UserProfile user = UserProfile.builder()
                .accountCreatedAt(existingTime)
                .build();
        user.onCreate();

        assertThat(user.getAccountCreatedAt()).isEqualTo(existingTime);
    }

    @Test
    @DisplayName("Should update updatedAt on preUpdate")
    void onUpdate_ShouldUpdateTimestamp() {
        LocalDateTime beforeUpdate = userProfile.getUpdatedAt();
        userProfile.onUpdate();

        assertThat(userProfile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("recordLogin should increment loginCount and update timestamps")
    void recordLogin_ShouldIncrementLoginCount() {
        int initialCount = userProfile.getLoginCount();
        userProfile.recordLogin();

        assertThat(userProfile.getLoginCount()).isEqualTo(initialCount + 1);
        assertThat(userProfile.getLastLoginAt()).isNotNull();
        assertThat(userProfile.getLastActivityAt()).isNotNull();
    }

    @Test
    @DisplayName("recordLogin should handle null loginCount")
    void recordLogin_WithNullLoginCount_ShouldSetToOne() {
        UserProfile newUser = new UserProfile();
        newUser.recordLogin();

        assertThat(newUser.getLoginCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("recordActivity should update lastActivityAt")
    void recordActivity_ShouldUpdateLastActivityAt() {
        userProfile.recordActivity();

        assertThat(userProfile.getLastActivityAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set blocked fields correctly")
    void setBlocked_ShouldUpdateFields() {
        userProfile.setBlocked(true);
        userProfile.setBlockedAt(LocalDateTime.now());
        userProfile.setBlockedReason("Suspicious activity");

        assertThat(userProfile.isBlocked()).isTrue();
        assertThat(userProfile.getBlockedReason()).isEqualTo("Suspicious activity");
    }

    @Test
    @DisplayName("Should set reactivation token fields")
    void setReactivationToken_ShouldUpdateFields() {
        String token = "test-token-123";
        userProfile.setReactivationToken(token);
        userProfile.setReactivationTokenExpiry(LocalDateTime.now().plusHours(24));

        assertThat(userProfile.getReactivationToken()).isEqualTo(token);
        assertThat(userProfile.getReactivationTokenExpiry()).isNotNull();
    }

    @Test
    @DisplayName("Should set address and contact fields")
    void setAddressFields_ShouldUpdate() {
        userProfile.setAddress("123 Main St");
        userProfile.setCity("Tunis");
        userProfile.setCountry("Tunisia");
        userProfile.setPhoneNumber("12345678");
        userProfile.setDateOfBirth(LocalDateTime.of(1990, 1, 1, 0, 0));

        assertThat(userProfile.getAddress()).isEqualTo("123 Main St");
        assertThat(userProfile.getCity()).isEqualTo("Tunis");
        assertThat(userProfile.getCountry()).isEqualTo("Tunisia");
        assertThat(userProfile.getPhoneNumber()).isEqualTo("12345678");
        assertThat(userProfile.getDateOfBirth()).isNotNull();
    }
}