package tn.esprit.usermanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.usermanagementservice.client.AuthServiceClient;
import tn.esprit.usermanagementservice.dto.*;
import tn.esprit.usermanagementservice.entity.LoginHistory;
import tn.esprit.usermanagementservice.entity.Role;
import tn.esprit.usermanagementservice.entity.UserProfile;
import tn.esprit.usermanagementservice.repository.LoginHistoryRepository;
import tn.esprit.usermanagementservice.repository.UserProfileRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import tn.esprit.usermanagementservice.entity.ProfileChangeHistory;
import tn.esprit.usermanagementservice.repository.ProfileChangeHistoryRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {

    private static final int SUSPICIOUS_COUNTRY_CHANGE_THRESHOLD = 5;
    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final String TIME_FORMAT = "HH:mm:ss";
    private static final String TOPIC_LOGINS = "/topic/logins";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_NOT_FOUND_EMAIL = "User not found with email: ";
    private static final String USER_NOT_FOUND_ID = "User not found with id: ";
    private static final String USER_NOT_FOUND = "User not found: ";

    private final UserProfileRepository userProfileRepository;
    private final AuthServiceClient authServiceClient;
    private final KeycloakAdminService keycloakAdminService;
    private final ProfileChangeHistoryRepository profileChangeHistoryRepository;
    private final PhoneNumberValidator phoneNumberValidator;
    private final LoginHistoryRepository loginHistoryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private UserProfileService self;

    @Value("${app.mail.from:noreply@wallstreetenglish.com}")
    private String mailFrom;

    @Value("${app.admin.email:admin@wallstreetenglish.com}")
    private String adminEmail;

    private static ThreadLocal<String> creatingUsers = new ThreadLocal<>();

    // ── Profile creation ─────────────────────────────────────────────────────

    @Transactional
    public void createMinimalProfile(String email, Role role) {
        creatingUsers.set(email);
        try {
            if (userProfileRepository.existsByEmail(email)) {
                log.warn("⚠️ Profile already exists for: {}", email);
                return;
            }
            UserProfile userProfile = UserProfile.builder()
                    .email(email)
                    .firstName("")
                    .lastName("")
                    .role(role)
                    .active(true)
                    .createdBy("auth-service")
                    .build();
            userProfileRepository.save(userProfile);
            log.info("✅ Minimal profile created for: {}", email);
        } finally {
            creatingUsers.remove();
        }
    }

    @Transactional
    public UserProfileDTO createUser(CreateUserRequest request, String adminToken) {
        creatingUsers.set(request.getEmail());
        try {
            if (userProfileRepository.existsByEmail(request.getEmail())) {
                throw new IllegalStateException("Email already exists in user management service");
            }

            UserProfile userProfile = UserProfile.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName() != null ? request.getFirstName() : "")
                    .lastName(request.getLastName() != null ? request.getLastName() : "")
                    .phoneNumber(request.getPhoneNumber())
                    .dateOfBirth(request.getDateOfBirth())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .country(request.getCountry())
                    .role(request.getRole())
                    .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin")
                    .active(true)
                    .build();

            UserProfile savedProfile = userProfileRepository.save(userProfile);
            log.info("✅ User profile created with ID: {}", savedProfile.getId());

            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                createUserInAuthService(request, adminToken, savedProfile);
            }

            return convertToDTO(savedProfile);
        } finally {
            creatingUsers.remove();
        }
    }

    private void createUserInAuthService(CreateUserRequest request, String adminToken, UserProfile savedProfile) {
        try {
            AuthRegisterRequest authRequest = new AuthRegisterRequest();
            authRequest.setEmail(request.getEmail());
            authRequest.setPassword(request.getPassword());
            authRequest.setConfirmPassword(request.getPassword());
            authRequest.setRole(request.getRole().name());
            authRequest.setFirstName(request.getFirstName());
            authRequest.setLastName(request.getLastName());

            String cleanToken = (adminToken != null && !adminToken.startsWith(BEARER_PREFIX))
                    ? BEARER_PREFIX + adminToken
                    : adminToken;

            AuthResponse response = authServiceClient.createUserByAdmin(authRequest, cleanToken);
            log.info("✅ Auth Service Response: {}", response);
        } catch (Exception e) {
            log.error("❌ Failed to create user in Auth Service: {}", e.getMessage());
            userProfileRepository.delete(savedProfile);
            throw new IllegalStateException("Failed to create user in auth service: " + e.getMessage());
        }
    }


    // ── Profile updates ──────────────────────────────────────────────────────

    @Transactional
    public UserProfileDTO updateUserProfile(String email, UpdateUserRequest request) {
        UserProfile userProfile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_EMAIL + email));

        Role oldRole = userProfile.getRole();
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));

        applySimpleFieldUpdates(email, request, userProfile, timeStr);
        applyCountryUpdate(email, request, userProfile, timeStr);
        applyRoleAndStatusUpdates(email, request, userProfile, oldRole, timeStr);

        userProfile.recordActivity();
        UserProfile updatedProfile = userProfileRepository.save(userProfile);
        log.info("Profile updated for: {}", email);
        return convertToDTO(updatedProfile);
    }

    private void applySimpleFieldUpdates(String email, UpdateUserRequest request,
                                          UserProfile userProfile, String timeStr) {
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()
                && !request.getFirstName().equals(userProfile.getFirstName())) {
            String old = userProfile.getFirstName();
            userProfile.setFirstName(request.getFirstName());
            broadcastProfileChange(email, "first name", old, request.getFirstName(), timeStr);
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()
                && !request.getLastName().equals(userProfile.getLastName())) {
            String old = userProfile.getLastName();
            userProfile.setLastName(request.getLastName());
            broadcastProfileChange(email, "last name", old, request.getLastName(), timeStr);
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(userProfile.getPhoneNumber())) {
            String old = userProfile.getPhoneNumber();
            userProfile.setPhoneNumber(request.getPhoneNumber());
            broadcastProfileChange(email, "phone number", old, request.getPhoneNumber(), timeStr);
        }
        if (request.getAddress() != null && !request.getAddress().equals(userProfile.getAddress())) {
            String old = userProfile.getAddress();
            userProfile.setAddress(request.getAddress());
            broadcastProfileChange(email, "address", old, request.getAddress(), timeStr);
        }
        if (request.getCity() != null && !request.getCity().equals(userProfile.getCity())) {
            String old = userProfile.getCity();
            userProfile.setCity(request.getCity());
            broadcastProfileChange(email, "city", old, request.getCity(), timeStr);
        }
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().equals(userProfile.getDateOfBirth())) {
            String old = userProfile.getDateOfBirth() != null ? userProfile.getDateOfBirth().toString() : "null";
            userProfile.setDateOfBirth(request.getDateOfBirth());
            broadcastProfileChange(email, "date of birth", old, request.getDateOfBirth().toString(), timeStr);
        }
    }

    private void applyCountryUpdate(String email, UpdateUserRequest request,
                                     UserProfile userProfile, String timeStr) {
        if (request.getCountry() == null) return;

        String oldCountry = userProfile.getCountry();
        userProfile.setCountry(request.getCountry());

        if (oldCountry != null && !oldCountry.equals(request.getCountry())) {
            ProfileChangeHistory history = ProfileChangeHistory.builder()
                    .email(email)
                    .fieldChanged("country")
                    .oldValue(oldCountry)
                    .newValue(request.getCountry())
                    .changedAt(LocalDateTime.now())
                    .build();
            profileChangeHistoryRepository.save(history);
            log.info("Country change saved: {} from {} to {}", email, oldCountry, request.getCountry());
            checkAndAlertSuspiciousCountryChanges(email);
        } else if (oldCountry == null) {
            log.info("First country set for {}: {}", email, request.getCountry());
        }

        broadcastProfileChange(email, "country",
                oldCountry != null ? oldCountry : "Not set", request.getCountry(), timeStr);
    }

    private void applyRoleAndStatusUpdates(String email, UpdateUserRequest request,
                                            UserProfile userProfile, Role oldRole, String timeStr) {
        if (request.getRole() != null && request.getRole() != oldRole) {
            String old = oldRole != null ? oldRole.name() : "null";
            userProfile.setRole(request.getRole());
            broadcastProfileChange(email, "role", old, request.getRole().name(), timeStr);
        }
        if (request.getActive() != null && request.getActive() != userProfile.isActive()) {
            String old = String.valueOf(userProfile.isActive());
            userProfile.setActive(request.getActive());
            broadcastProfileChange(email, "active status", old, String.valueOf(request.getActive()), timeStr);
        }
    }

    private void checkAndAlertSuspiciousCountryChanges(String email) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        int countryChangesToday = profileChangeHistoryRepository.countCountryChangesToday(email, startOfDay);

        if (countryChangesToday >= SUSPICIOUS_COUNTRY_CHANGE_THRESHOLD) {
            blockUser(email, "Suspicious: " + countryChangesToday + " country changes in one day");
            String alertMessage = String.format("SUSPICIOUS & BLOCKED: %s changed country %d times today. Account blocked.",
                    email, countryChangesToday);
            messagingTemplate.convertAndSend(TOPIC_LOGINS, alertMessage);
            log.warn("Suspicious activity detected: {}", alertMessage);
        }
    }

    private void broadcastProfileChange(String email, String field, String oldValue, String newValue, String timeStr) {
        String changeMessage = String.format("PROFILE CHANGE: %s changed %s from '%s' to '%s' at %s",
                email, field, oldValue, newValue, timeStr);
        messagingTemplate.convertAndSend(TOPIC_LOGINS, changeMessage);
        log.info("Broadcast: {}", changeMessage);
    }

    @Transactional
    public UserProfileDTO updateUser(Long id, UpdateUserRequest request) {
        UserProfile userProfile = userProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_ID + id));

        Role oldRole = userProfile.getRole();

        if (request.getFirstName() != null)   userProfile.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    userProfile.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) userProfile.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)     userProfile.setAddress(request.getAddress());
        if (request.getCity() != null)        userProfile.setCity(request.getCity());
        if (request.getCountry() != null)     userProfile.setCountry(request.getCountry());
        if (request.getActive() != null)      userProfile.setActive(request.getActive());
        if (request.getDateOfBirth() != null) userProfile.setDateOfBirth(request.getDateOfBirth());

        boolean roleChanged = applyRoleChange(userProfile, request, oldRole);

        userProfile.recordActivity();
        UserProfile updatedProfile = userProfileRepository.save(userProfile);

        if (roleChanged) {
            forceLogoutAfterRoleChange(userProfile);
        }

        return convertToDTO(updatedProfile);
    }

    private boolean applyRoleChange(UserProfile userProfile, UpdateUserRequest request, Role oldRole) {
        if (request.getRole() == null || request.getRole() == oldRole) {
            return false;
        }
        log.info("Role changing from {} to {} for {}", oldRole, request.getRole(), userProfile.getEmail());
        userProfile.setRole(request.getRole());

        try {
            keycloakAdminService.updateUserRole(userProfile.getEmail(), request.getRole().name());
            log.info("Role updated in Keycloak for: {}", userProfile.getEmail());
        } catch (Exception e) {
            log.error("Failed to update role in Keycloak: {}", e.getMessage());
        }

        try {
            String adminToken = extractAdminToken();
            if (adminToken == null) {
                log.warn("No admin token found, skipping auth service role update");
            } else {
                RoleUpdateRequest roleUpdate = new RoleUpdateRequest();
                roleUpdate.setEmail(userProfile.getEmail());
                roleUpdate.setRole(request.getRole());
                authServiceClient.updateUserRole(roleUpdate, BEARER_PREFIX + adminToken);
                log.info("Role updated in Auth Service for: {}", userProfile.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to update role in Auth Service: {}", e.getMessage());
        }
        return true;
    }

    private void forceLogoutAfterRoleChange(UserProfile userProfile) {
        try {
            keycloakAdminService.logoutUserSessions(userProfile.getEmail());
            publishLogoutEvent(userProfile.getEmail(), userProfile.getRole().name(), LoginHistory.LogoutType.FORCED);
            log.info("User force-logged out after role change: {}", userProfile.getEmail());
        } catch (Exception e) {
            log.error("Could not force-logout user: {}", e.getMessage());
        }
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public UserProfileDTO getUserById(Long id) {
        return convertToDTO(userProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_ID + id)));
    }

    public UserProfileDTO getUserByEmail(String email) {
        return convertToDTO(userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_EMAIL + email)));
    }

    public List<UserProfileDTO> getAllUsers() {
        return userProfileRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<UserProfileDTO> getUsersByRole(Role role) {
        return userProfileRepository.findByRole(role).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userProfileRepository.existsById(id))
            throw new IllegalArgumentException(USER_NOT_FOUND_ID + id);
        userProfileRepository.deleteById(id);
    }

    // ── Login / Logout recording ─────────────────────────────────────────────

    @Transactional
    public void recordUserLogin(String email) {
        UserProfile userProfile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_EMAIL + email));

        if (userProfile.isBlocked()) {
            throw new IllegalStateException("Your account is blocked. Please check your email for reactivation instructions.");
        }

        userProfile.recordLogin();
        userProfileRepository.save(userProfile);
        log.info("Login stats updated for: {}", email);
    }

    @Transactional
    public void recordUserLogout(String email, LoginHistory.LogoutType logoutType) {
        UserProfile userProfile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_EMAIL + email));

        publishLogoutEvent(email, userProfile.getRole().name(), logoutType);
        log.info("Logout recorded for: {} ({})", email, logoutType);
    }

    // ── Auth sync ─────────────────────────────────────────────────────────────

    @Transactional
    public UserProfileDTO syncUserFromAuth(String email, String role) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByEmail(email);
        if (existingProfile.isPresent()) {
            log.info("User already exists, returning: {}", email);
            return convertToDTO(existingProfile.get());
        }

        UserProfile newProfile = UserProfile.builder()
                .email(email)
                .firstName("")
                .lastName("")
                .role(Role.valueOf(role))
                .active(true)
                .createdBy("auth-sync")
                .build();

        UserProfile savedProfile = userProfileRepository.save(newProfile);
        log.info("User synced from Auth and created: {}", email);
        return convertToDTO(savedProfile);
    }

    public void forceUserLogout(String email) {
        keycloakAdminService.logoutUserSessions(email);
        publishLogoutEvent(email, null, LoginHistory.LogoutType.FORCED);
    }

    // ── Block/Unblock methods ─────────────────────────────────────────────

    @Transactional
    public void blockUser(String email, String reason) {
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND + email));

        user.setBlocked(true);
        user.setBlockedAt(LocalDateTime.now());
        user.setBlockedReason(reason);
        user.setActive(false);
        userProfileRepository.save(user);

        self.sendReactivationEmail(email);

        String alertMessage = String.format("ACCOUNT BLOCKED: %s - %s at %s. Reactivation email sent.",
                email, reason, LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT)));
        messagingTemplate.convertAndSend(TOPIC_LOGINS, alertMessage);
        log.info("User blocked: {}", email);
    }

    // ── Reactivation methods ─────────────────────────────────────────────

    @Transactional
    public void sendReactivationEmail(String email) {
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND + email));

        // Generate unique token
        String token = UUID.randomUUID().toString();
        user.setReactivationToken(token);
        user.setReactivationTokenExpiry(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        userProfileRepository.save(user);

        // Send email to student
        String reactivationLink = "http://192.168.168.128:32708/reactivate?token=" + token + "&email=" + email;

        String subject = "Account Blocked - Reactivation Required";
        String body = String.format("""
        Hello %s,
        
        Your account has been blocked due to suspicious activity (multiple country changes in one day).
        
        If this was YOU, please click the link below to request reactivation:
        %s
        
        If this was NOT you, please ignore this email and contact support immediately.
        
        Best regards,
        English School Admin
        """,
                user.getFirstName() != null && !user.getFirstName().isEmpty() ? user.getFirstName() : user.getEmail(),
                reactivationLink
        );

        sendEmail(email, subject, body);
        log.info("Reactivation email sent to: {}", email);
    }

    @Transactional
    public boolean validateReactivationToken(String token) {
        Optional<UserProfile> user = userProfileRepository.findAll().stream()
                .filter(u -> token.equals(u.getReactivationToken()))
                .findFirst();

        return user.isPresent()
                && user.get().getReactivationTokenExpiry() != null
                && user.get().getReactivationTokenExpiry().isAfter(LocalDateTime.now());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void publishLogoutEvent(String email, String role, LoginHistory.LogoutType logoutType) {
        LoginEventMessage event = new LoginEventMessage();
        event.setEmail(email);
        event.setRole(role);
        event.setType(LoginEventMessage.EventType.LOGOUT);
        event.setLogoutType(convertLogoutType(logoutType));
        event.setTimestamp(LocalDateTime.now());
        event.setIpAddress(getClientIpAddress());
        rabbitTemplate.convertAndSend("login-events", event);
    }

    private LoginEventMessage.LogoutType convertLogoutType(LoginHistory.LogoutType type) {
        if (type == null) return LoginEventMessage.LogoutType.VOLUNTARY;
        switch (type) {
            case VOLUNTARY: return LoginEventMessage.LogoutType.VOLUNTARY;
            case TIMEOUT: return LoginEventMessage.LogoutType.TIMEOUT;
            case FORCED: return LoginEventMessage.LogoutType.FORCED;
            default: return LoginEventMessage.LogoutType.VOLUNTARY;
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Could not get client IP: {}", e.getMessage());
        }
        return "127.0.0.1";
    }

    private String extractAdminToken() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String authHeader = attributes.getRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                return authHeader.substring(BEARER_PREFIX.length());
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            return auth.getCredentials().toString();
        }
        return null;
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private UserProfileDTO convertToDTO(UserProfile userProfile) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(userProfile.getId());
        dto.setEmail(userProfile.getEmail());
        dto.setFirstName(userProfile.getFirstName());
        dto.setLastName(userProfile.getLastName());
        dto.setPhoneNumber(userProfile.getPhoneNumber());
        dto.setRole(userProfile.getRole());
        dto.setCreatedAt(userProfile.getCreatedAt());
        dto.setUpdatedAt(userProfile.getUpdatedAt());
        dto.setLastLoginAt(userProfile.getLastLoginAt());
        dto.setAccountCreatedAt(userProfile.getAccountCreatedAt());
        dto.setLastActivityAt(userProfile.getLastActivityAt());
        dto.setActive(userProfile.isActive());
        dto.setLoginCount(userProfile.getLoginCount());
        dto.setCreatedBy(userProfile.getCreatedBy());
        dto.setDateOfBirth(userProfile.getDateOfBirth());
        dto.setAddress(userProfile.getAddress());
        dto.setCity(userProfile.getCity());
        dto.setCountry(userProfile.getCountry());
        dto.setBlocked(userProfile.isBlocked());
        dto.setBlockedReason(userProfile.getBlockedReason());
        return dto;
    }




    @Transactional
    public void unblockUser(String email) {
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND + email));

        user.setBlocked(false);
        user.setBlockedAt(null);
        user.setBlockedReason(null);
        user.setActive(true);
        userProfileRepository.save(user);

        String studentSubject = "Your Account Has Been Reactivated";
        String studentBody = String.format("""
    Hello %s,
    
    Great news! Your account has been successfully reactivated by the admin.
    
    You can now log in to your account: http://192.168.168.128:32708/login
    
    If you have any issues, please contact support.
    
    Best regards,
    English School Admin
    """,
                user.getFirstName() != null && !user.getFirstName().isEmpty() ? user.getFirstName() : user.getEmail()
        );
        sendEmail(email, studentSubject, studentBody);
        log.info("Reactivation confirmation email sent to student: {}", email);

        String alertMessage = String.format("ACCOUNT UNBLOCKED: %s at %s. Student notified.",
                email, LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT)));
        messagingTemplate.convertAndSend(TOPIC_LOGINS, alertMessage);
        log.info("User unblocked: {}", email);
    }

    @Transactional
    public void processReactivationRequest(Map<String, String> request) {
        String email = request.get("email");
        String reason = request.get("reason");
        String confirmation = request.get("confirmation");

        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND + email));

        // Send email to ADMIN with Angular link
        String subject = "Reactivation Request from " + email;
        String body = String.format("""
    STUDENT REACTIVATION REQUEST
    
    Student Email: %s
    Student Name: %s
    Reason provided: %s
    Confirmation: %s
    
    To approve and unblock, click the link below:
    http://192.168.168.128:32708/admin/unblock/%s
    
    Or go to admin dashboard to unblock manually.
    """,
                email,
                user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : ""),
                reason,
                confirmation,
                email
        );

        sendEmail(adminEmail, subject, body);
        log.info("Reactivation request sent to admin for: {}", email);
    }
}