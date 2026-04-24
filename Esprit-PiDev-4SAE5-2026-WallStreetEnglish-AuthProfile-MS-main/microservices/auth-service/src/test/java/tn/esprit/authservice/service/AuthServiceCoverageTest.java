package tn.esprit.authservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.authservice.client.UserServiceClient;
import tn.esprit.authservice.dto.AuthResponse;
import tn.esprit.authservice.dto.LoginRequest;
import tn.esprit.authservice.dto.RegisterRequest;
import tn.esprit.authservice.entity.Role;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceCoverageTest {

    // ========== MOCKS ==========
    @Mock private UserRepository userRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private Keycloak keycloakAdmin;
    @Mock private WebClient webClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private HttpServletRequest httpServletRequest;

    // Keycloak resource mocks
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;
    @Mock private RoleMappingResource roleMappingResource;
    @Mock private RoleScopeResource roleScopeResource;
    @Mock private RolesResource rolesResource;
    @Mock private RoleResource roleResource;

    @InjectMocks private AuthService authService;

    // ========== TEST DATA CONSTANTS ==========
    private static final String TEST_REALM = "test-realm";
    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_SERVER_URL = "http://localhost:8080/auth";
    private static final String TEST_KEYCLOAK_ID = "kc-id-123";
    private static final String TEST_EMAIL = "user@test.com";
    private static final String TEST_PASSWORD = "pass123";
    private static final String TEST_ACCESS_TOKEN = "mock-access-token";
    private static final String TEST_REFRESH_TOKEN = "mock-refresh-token";

    // ========== SETUP ==========
    @BeforeEach
    void setUp() throws Exception {
        injectPrivateFields();
        setupMockWebClientDefaults();
    }

    private void injectPrivateFields() throws Exception {
        setField("realm", TEST_REALM);
        setField("clientId", TEST_CLIENT_ID);
        setField("serverUrl", TEST_SERVER_URL);
    }

    private void setField(String fieldName, String value) throws Exception {
        Field field = AuthService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(authService, value);
    }

    private void setupMockWebClientDefaults() {
        // This prevents NullPointerException when webClient is not explicitly mocked for a test
        lenient().when(webClient.post()).thenReturn(mock(WebClient.RequestBodyUriSpec.class));
        lenient().when(webClient.get()).thenReturn(mock(WebClient.RequestHeadersUriSpec.class));
    }

    // ========== HELPER METHODS ==========

    private void mockWebClientTokenSuccess() {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("access_token", TEST_ACCESS_TOKEN);
        tokenMap.put("refresh_token", TEST_REFRESH_TOKEN);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(tokenMap));
    }

    private void mockWebClientTokenFailure() {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());
    }

    private void mockWebClientGetBlocked(boolean blocked) {
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        Map<String, Object> body = Map.of("blocked", blocked);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(body));
    }

    private void mockWebClientGetBlockedError() {
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenThrow(new RuntimeException("Service unavailable"));
    }

    private void mockKeycloakCreateUser(int status) throws Exception {
        URI location = new URI("http://localhost/users/" + TEST_KEYCLOAK_ID);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getLocation()).thenReturn(location);

        when(keycloakAdmin.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(response);
    }

    private void mockKeycloakRoleAssign(String roleName) {
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(roleName);

        when(keycloakAdmin.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(TEST_KEYCLOAK_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        doNothing().when(roleScopeResource).add(anyList());
    }

    private void mockKeycloakRoleAssignError(String roleName) {
        when(keycloakAdmin.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(roleName)).thenThrow(new RuntimeException("Role not found"));
    }

    private void stubHttpServletRequest() {
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120");
    }

    private User buildUser(String email, Role role) {
        return User.builder()
                .id(1L)
                .email(email)
                .password("")
                .role(role)
                .keycloakId(TEST_KEYCLOAK_ID)
                .active(true)
                .emailVerified(true)
                .build();
    }

    private User buildUserWithCustomId(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .password("")
                .role(role)
                .keycloakId(TEST_KEYCLOAK_ID)
                .active(true)
                .emailVerified(true)
                .build();
    }

    private RegisterRequest createRegisterRequest(String email, Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(TEST_PASSWORD);
        req.setConfirmPassword(TEST_PASSWORD);
        req.setRole(role);
        req.setFirstName("John");
        req.setLastName("Doe");
        return req;
    }

    private LoginRequest createLoginRequest(String email) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(TEST_PASSWORD);
        return req;
    }

    // ========== REGISTER TESTS ==========
    @Nested
    class RegisterTests {

        @Test
        void register_emailAlreadyExists_throwsException() {
            // Given
            RegisterRequest req = createRegisterRequest("existing@test.com", Role.STUDENT);
            when(userRepository.findByEmail("existing@test.com"))
                    .thenReturn(Optional.of(buildUser("existing@test.com", Role.STUDENT)));

            // When & Then
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        void register_success() throws Exception {
            // Given
            RegisterRequest req = createRegisterRequest("newuser@test.com", Role.STUDENT);

            when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());
            mockKeycloakCreateUser(201);
            mockKeycloakRoleAssign("STUDENT");

            User savedUser = buildUser("newuser@test.com", Role.STUDENT);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            mockWebClientTokenSuccess();

            // When
            AuthResponse response = authService.register(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Registration successful");
            assertThat(response.getToken()).isEqualTo(TEST_ACCESS_TOKEN);

            verify(userRepository).save(any(User.class));
            verify(userServiceClient).createProfile(anyString(), anyString(), any(), any());
        }

        @Test
        void register_keycloakCreationFails_throwsException() throws Exception {
            // Given
            RegisterRequest req = createRegisterRequest("newuser@test.com", Role.STUDENT);

            when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());
            mockKeycloakCreateUser(500);

            // When & Then
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Registration failed");

            verify(userRepository, never()).save(any());
        }

        @Test
        void register_profileCreationFails_stillSucceeds() throws Exception {
            // Given
            RegisterRequest req = createRegisterRequest("newuser@test.com", Role.STUDENT);

            when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());
            mockKeycloakCreateUser(201);
            mockKeycloakRoleAssign("STUDENT");

            User savedUser = buildUser("newuser@test.com", Role.STUDENT);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            mockWebClientTokenSuccess();

            doThrow(new RuntimeException("Profile service down"))
                    .when(userServiceClient).createProfile(anyString(), anyString(), any(), any());

            // When
            AuthResponse response = authService.register(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Registration successful");

            verify(userServiceClient).createProfile(anyString(), anyString(), any(), any());
        }
    }

    // ========== LOGIN TESTS ==========
    @Nested
    class LoginTests {

        @Test
        void login_success() {
            // Given
            LoginRequest req = createLoginRequest(TEST_EMAIL);
            User user = buildUser(TEST_EMAIL, Role.STUDENT);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            mockWebClientTokenSuccess();
            mockWebClientGetBlocked(false);
            stubHttpServletRequest();

            // When
            AuthResponse response = authService.login(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Login successful");
            assertThat(response.getToken()).isEqualTo(TEST_ACCESS_TOKEN);

            verify(userServiceClient).recordUserLogin(TEST_EMAIL);
            verify(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        void login_userNotFound_throwsException() {
            // Given
            LoginRequest req = createLoginRequest("ghost@test.com");
            mockWebClientTokenSuccess();
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        void login_tokenFails_throwsException() {
            // Given
            LoginRequest req = createLoginRequest(TEST_EMAIL);
            mockWebClientTokenFailure();

            // When & Then
            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        void login_recordLoginFails_stillSucceeds() {
            // Given
            LoginRequest req = createLoginRequest(TEST_EMAIL);
            User user = buildUser(TEST_EMAIL, Role.STUDENT);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            mockWebClientTokenSuccess();
            mockWebClientGetBlocked(false);
            stubHttpServletRequest();

            doThrow(new RuntimeException("Service down"))
                    .when(userServiceClient).recordUserLogin(anyString());

            // When
            AuthResponse response = authService.login(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Login successful");

            verify(userServiceClient).recordUserLogin(TEST_EMAIL);
        }

        @Test
        void login_rabbitMqFails_stillSucceeds() {
            // Given
            LoginRequest req = createLoginRequest(TEST_EMAIL);
            User user = buildUser(TEST_EMAIL, Role.STUDENT);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            mockWebClientTokenSuccess();
            mockWebClientGetBlocked(false);
            stubHttpServletRequest();

            doThrow(new RuntimeException("RabbitMQ down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

            // When
            AuthResponse response = authService.login(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Login successful");

            verify(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        void login_blockedCheckFails_proceedsAnyway() {
            // Given
            LoginRequest req = createLoginRequest(TEST_EMAIL);
            User user = buildUser(TEST_EMAIL, Role.STUDENT);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            mockWebClientTokenSuccess();
            mockWebClientGetBlockedError();
            stubHttpServletRequest();

            // When
            AuthResponse response = authService.login(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Login successful");
        }
    }

    // ========== LOGOUT TESTS ==========
    @Nested
    class LogoutTests {

        @Test
        void logout_success_returnsResponse() {
            // Given
            User user = buildUser(TEST_EMAIL, Role.STUDENT);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            stubHttpServletRequest();

            // When
            AuthResponse response = authService.logout(TEST_EMAIL, "VOLUNTARY");

            // Then
            assertThat(response.getMessage()).isEqualTo("Logout successful");
            verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        void logout_userNotFound_throwsException() {
            // Given
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.logout("ghost@test.com", "VOLUNTARY"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Logout failed");
        }

        @Test
        void logout_rabbitMqFails_stillReturnsSuccess() {
            // Given
            User user = buildUser(TEST_EMAIL, Role.STUDENT);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            stubHttpServletRequest();

            doThrow(new RuntimeException("rabbit down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

            // When
            AuthResponse response = authService.logout(TEST_EMAIL, "VOLUNTARY");

            // Then
            assertThat(response.getMessage()).isEqualTo("Logout successful");
            verify(rabbitTemplate).convertAndSend(anyString(), any(Object.class));
        }
    }
}