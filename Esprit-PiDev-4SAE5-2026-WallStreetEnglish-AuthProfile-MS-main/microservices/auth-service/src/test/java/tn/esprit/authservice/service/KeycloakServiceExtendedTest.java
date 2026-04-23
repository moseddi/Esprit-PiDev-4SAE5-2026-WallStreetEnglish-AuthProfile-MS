package tn.esprit.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakService Tests")
class KeycloakServiceExtendedTest {

    @Mock private Keycloak keycloakAdmin;
    @InjectMocks private KeycloakService keycloakService;

    // Deep mock chain
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;
    @Mock private ClientsResource clientsResource;
    @Mock private ClientResource clientResource;
    @Mock private RolesResource rolesResource;
    @Mock private RoleMappingResource roleMappingResource;
    @Mock private RoleScopeResource roleScopeResource;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakService, "realm", "myapp2");
        ReflectionTestUtils.setField(keycloakService, "clientId", "angular-app");
    }

    @Nested
    @DisplayName("updateUserRole")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should throw when user not found in Keycloak")
        void updateUserRole_UserNotFound_ThrowsException() {
            when(keycloakAdmin.realm("myapp2")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("student@test.com")).thenReturn(Collections.emptyList());

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    keycloakService.updateUserRole("student@test.com", "TUTOR"));

            assertTrue(ex.getMessage().contains("Failed to update role in Keycloak"));
        }

        @Test
        @DisplayName("Should throw when client not found in Keycloak")
        void updateUserRole_ClientNotFound_ThrowsException() {
            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setId("user-id-123");
            kcUser.setEmail("student@test.com");

            when(keycloakAdmin.realm("myapp2")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("student@test.com")).thenReturn(List.of(kcUser));
            when(realmResource.clients()).thenReturn(clientsResource);
            when(clientsResource.findByClientId("angular-app")).thenReturn(Collections.emptyList());

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    keycloakService.updateUserRole("student@test.com", "TUTOR"));

            assertTrue(ex.getMessage().contains("Failed to update role in Keycloak"));
        }

        @Test
        @DisplayName("Should throw when target role not found in Keycloak client")
        void updateUserRole_RoleNotFound_ThrowsException() {
            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setId("user-id-123");

            ClientRepresentation client = new ClientRepresentation();
            client.setId("client-uuid");

            RoleRepresentation otherRole = new RoleRepresentation();
            otherRole.setName("STUDENT");

            when(keycloakAdmin.realm("myapp2")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("student@test.com")).thenReturn(List.of(kcUser));
            when(realmResource.clients()).thenReturn(clientsResource);
            when(clientsResource.findByClientId("angular-app")).thenReturn(List.of(client));
            when(clientsResource.get("client-uuid")).thenReturn(clientResource);
            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.list()).thenReturn(List.of(otherRole));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    keycloakService.updateUserRole("student@test.com", "ADMIN"));

            assertTrue(ex.getMessage().contains("Failed to update role in Keycloak"));
        }

        @Test
        @DisplayName("Should successfully update role when all Keycloak resources exist")
        void updateUserRole_AllResourcesExist_ShouldSucceed() {
            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setId("user-id-123");

            ClientRepresentation client = new ClientRepresentation();
            client.setId("client-uuid");

            RoleRepresentation targetRole = new RoleRepresentation();
            targetRole.setName("TUTOR");

            RoleRepresentation currentRole = new RoleRepresentation();
            currentRole.setName("STUDENT");

            when(keycloakAdmin.realm("myapp2")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("student@test.com")).thenReturn(List.of(kcUser));
            when(realmResource.clients()).thenReturn(clientsResource);
            when(clientsResource.findByClientId("angular-app")).thenReturn(List.of(client));
            when(clientsResource.get("client-uuid")).thenReturn(clientResource);
            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.list()).thenReturn(List.of(targetRole, currentRole));
            when(usersResource.get("user-id-123")).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.clientLevel("client-uuid")).thenReturn(roleScopeResource);
            when(roleScopeResource.listAll()).thenReturn(List.of(currentRole));
            doNothing().when(roleScopeResource).remove(anyList());
            doNothing().when(roleScopeResource).add(anyList());
            doNothing().when(userResource).logout();

            assertDoesNotThrow(() -> keycloakService.updateUserRole("student@test.com", "TUTOR"));

            verify(roleScopeResource).remove(List.of(currentRole));
            verify(roleScopeResource).add(List.of(targetRole));
            verify(userResource).logout();
        }

        @Test
        @DisplayName("Should skip remove step when user has no current roles")
        void updateUserRole_NoCurrentRoles_ShouldSkipRemove() {
            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setId("user-id-123");

            ClientRepresentation client = new ClientRepresentation();
            client.setId("client-uuid");

            RoleRepresentation targetRole = new RoleRepresentation();
            targetRole.setName("TUTOR");

            when(keycloakAdmin.realm("myapp2")).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.search("student@test.com")).thenReturn(List.of(kcUser));
            when(realmResource.clients()).thenReturn(clientsResource);
            when(clientsResource.findByClientId("angular-app")).thenReturn(List.of(client));
            when(clientsResource.get("client-uuid")).thenReturn(clientResource);
            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.list()).thenReturn(List.of(targetRole));
            when(usersResource.get("user-id-123")).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.clientLevel("client-uuid")).thenReturn(roleScopeResource);
            when(roleScopeResource.listAll()).thenReturn(Collections.emptyList());
            doNothing().when(roleScopeResource).add(anyList());
            doNothing().when(userResource).logout();

            assertDoesNotThrow(() -> keycloakService.updateUserRole("student@test.com", "TUTOR"));

            verify(roleScopeResource, never()).remove(anyList());
            verify(roleScopeResource).add(List.of(targetRole));
        }
    }
}