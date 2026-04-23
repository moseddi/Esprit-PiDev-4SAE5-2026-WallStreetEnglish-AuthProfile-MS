package tn.esprit.authservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tn.esprit.authservice.entity.Role;
import tn.esprit.authservice.entity.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository Extended Tests")
class UserRepositoryExtendedTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager entityManager;

    private User studentUser;
    private User tutorUser;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .email("student@repo.com")
                .password("hash")
                .role(Role.STUDENT)
                .keycloakId("kc-student")
                .active(true)
                .emailVerified(true)
                .build();

        tutorUser = User.builder()
                .email("tutor@repo.com")
                .password("hash2")
                .role(Role.TUTOR)
                .keycloakId("kc-tutor")
                .active(true)
                .emailVerified(false)
                .build();

        entityManager.persistAndFlush(studentUser);
        entityManager.persistAndFlush(tutorUser);
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true for existing email")
        void existsByEmail_Existing_ReturnsTrue() {
            assertThat(userRepository.existsByEmail("student@repo.com")).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent email")
        void existsByEmail_NonExistent_ReturnsFalse() {
            assertThat(userRepository.existsByEmail("ghost@repo.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("findByKeycloakId")
    class FindByKeycloakIdTests {

        @Test
        @DisplayName("Should find user by keycloakId")
        void findByKeycloakId_Existing_ReturnsUser() {
            Optional<User> found = userRepository.findByKeycloakId("kc-student");
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("student@repo.com");
        }

        @Test
        @DisplayName("Should return empty for unknown keycloakId")
        void findByKeycloakId_NonExistent_ReturnsEmpty() {
            Optional<User> found = userRepository.findByKeycloakId("unknown-kc-id");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should find user by id")
        void findById_Existing_ReturnsUser() {
            Optional<User> found = userRepository.findById(studentUser.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getRole()).isEqualTo(Role.STUDENT);
        }

        @Test
        @DisplayName("Should return empty for non-existent id")
        void findById_NonExistent_ReturnsEmpty() {
            Optional<User> found = userRepository.findById(99999L);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("save and update")
    class SaveAndUpdateTests {

        @Test
        @DisplayName("Should update user role")
        void save_UpdateRole_PersistsChange() {
            studentUser.setRole(Role.ADMIN);
            userRepository.save(studentUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> updated = userRepository.findByEmail("student@repo.com");
            assertThat(updated).isPresent();
            assertThat(updated.get().getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("Should deactivate user")
        void save_SetInactive_PersistsChange() {
            studentUser.setActive(false);
            userRepository.save(studentUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> updated = userRepository.findByEmail("student@repo.com");
            assertThat(updated).isPresent();
            assertThat(updated.get().isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("User entity UserDetails contract")
    class UserDetailsContractTests {

        @Test
        @DisplayName("isEnabled returns true when active and emailVerified")
        void isEnabled_ActiveAndVerified_ReturnsTrue() {
            assertThat(studentUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEnabled returns false when not active")
        void isEnabled_Inactive_ReturnsFalse() {
            studentUser.setActive(false);
            assertThat(studentUser.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("isEnabled returns false when email not verified")
        void isEnabled_NotVerified_ReturnsFalse() {
            assertThat(tutorUser.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("getUsername returns email")
        void getUsername_ReturnsEmail() {
            assertThat(studentUser.getUsername()).isEqualTo("student@repo.com");
        }

        @Test
        @DisplayName("getAuthorities returns ROLE_ prefixed role")
        void getAuthorities_ReturnsCorrectAuthority() {
            assertThat(studentUser.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_STUDENT");
        }

        @Test
        @DisplayName("Account non-expired, non-locked, credentials non-expired all return true")
        void accountStateFlags_AllTrue() {
            assertThat(studentUser.isAccountNonExpired()).isTrue();
            assertThat(studentUser.isAccountNonLocked()).isTrue();
            assertThat(studentUser.isCredentialsNonExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should delete user and not find it afterwards")
        void delete_ExistingUser_RemovesFromDB() {
            userRepository.delete(tutorUser);
            entityManager.flush();

            Optional<User> found = userRepository.findByEmail("tutor@repo.com");
            assertThat(found).isEmpty();
        }
    }
}