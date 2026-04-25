package tn.esprit.usermanagementservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProfileChangeHistory Entity Tests")
class ProfileChangeHistoryTest {

    private ProfileChangeHistory profileChangeHistory;

    @BeforeEach
    void setUp() {
        profileChangeHistory = ProfileChangeHistory.builder()
                .id(1L)
                .email("test@test.com")
                .fieldChanged("country")
                .oldValue("USA")
                .newValue("France")
                .changedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create ProfileChangeHistory with builder")
    void builder_ShouldCreateEntity() {
        assertThat(profileChangeHistory).isNotNull();
        assertThat(profileChangeHistory.getId()).isEqualTo(1L);
        assertThat(profileChangeHistory.getEmail()).isEqualTo("test@test.com");
        assertThat(profileChangeHistory.getFieldChanged()).isEqualTo("country");
        assertThat(profileChangeHistory.getOldValue()).isEqualTo("USA");
        assertThat(profileChangeHistory.getNewValue()).isEqualTo("France");
    }

    @Test
    @DisplayName("Should set changedAt on prePersist")
    void prePersist_ShouldSetChangedAt() {
        ProfileChangeHistory newHistory = new ProfileChangeHistory();
        newHistory.onCreate();

        assertThat(newHistory.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update fields via setters")
    void setters_ShouldUpdateFields() {
        ProfileChangeHistory history = new ProfileChangeHistory();
        history.setId(2L);
        history.setEmail("new@test.com");
        history.setFieldChanged("phone");
        history.setOldValue("123456789");
        history.setNewValue("987654321");
        history.setChangedAt(LocalDateTime.now());

        assertThat(history.getId()).isEqualTo(2L);
        assertThat(history.getEmail()).isEqualTo("new@test.com");
        assertThat(history.getFieldChanged()).isEqualTo("phone");
        assertThat(history.getOldValue()).isEqualTo("123456789");
        assertThat(history.getNewValue()).isEqualTo("987654321");
    }

    @Test
    @DisplayName("Should handle null values")
    void handleNullValues_ShouldNotThrow() {
        ProfileChangeHistory history = new ProfileChangeHistory();
        history.setEmail(null);
        history.setFieldChanged(null);

        assertThat(history.getEmail()).isNull();
        assertThat(history.getFieldChanged()).isNull();
    }
}