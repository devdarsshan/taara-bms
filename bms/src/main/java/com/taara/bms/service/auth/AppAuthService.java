package com.taara.bms.service.auth;

import com.taara.bms.config.AuthProperties;
import com.taara.bms.dto.auth.AppUserResponse;
import com.taara.bms.dto.auth.CurrentUserResponse;
import com.taara.bms.dto.auth.InviteUserRequest;
import com.taara.bms.dto.auth.SignupRequest;
import com.taara.bms.dto.common.MessageResponse;
import com.taara.bms.entity.auth.AppUser;
import com.taara.bms.enums.AppUserRole;
import com.taara.bms.enums.AppUserStatus;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.repo.auth.AppUserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AppAuthService {

    private static final Logger log = LoggerFactory.getLogger(AppAuthService.class);

    private final AppUserRepository appUserRepository;
    private final SupabaseAdminClient supabaseAdminClient;
    private final AuthProperties authProperties;

    public AppAuthService(
            AppUserRepository appUserRepository,
            SupabaseAdminClient supabaseAdminClient,
            AuthProperties authProperties
    ) {
        this.appUserRepository = appUserRepository;
        this.supabaseAdminClient = supabaseAdminClient;
        this.authProperties = authProperties;
    }

    @Transactional
    public MessageResponse signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.info("Completing invited signup. email='{}'", normalizedEmail);

        AppUser appUser = appUserRepository.findByEmailIgnoreCaseAndIsDeletedFalse(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("APP_USER_NOT_INVITED", "This email is not invited to use the application"));

        if (appUser.getStatus() == AppUserStatus.DISABLED) {
            throw new BusinessValidationException("APP_USER_DISABLED", "This account has been disabled", java.util.Map.of("email", normalizedEmail));
        }
        if (appUser.getStatus() == AppUserStatus.ACTIVE) {
            throw new BusinessValidationException("APP_USER_ALREADY_ACTIVE", "This account is already active. Please log in.", java.util.Map.of("email", normalizedEmail));
        }

        SupabaseAdminClient.SupabaseAdminUser createdUser = supabaseAdminClient.createUser(normalizedEmail, request.password(), appUser.getRole());
        appUser.setSupabaseUserId(createdUser.id());
        appUser.setStatus(AppUserStatus.ACTIVE);
        appUser.setActivatedAt(LocalDateTime.now());
        appUserRepository.save(appUser);

        log.info("Signup completed successfully. email='{}', role={}", normalizedEmail, appUser.getRole());
        return new MessageResponse("Signup completed successfully. You can log in now.");
    }

    @Transactional
    public AppUserResponse inviteUser(InviteUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.info("Creating or updating invited app user. email='{}', role={}", normalizedEmail, request.role());

        AppUser appUser = appUserRepository.findByEmailIgnoreCaseAndIsDeletedFalse(normalizedEmail)
                .orElseGet(AppUser::new);

        boolean newRecord = appUser.getId() == null;
        appUser.setEmail(normalizedEmail);
        appUser.setRole(request.role());
        if (newRecord || appUser.getInvitedAt() == null) {
            appUser.setInvitedAt(LocalDateTime.now());
        }
        if (appUser.getStatus() != AppUserStatus.ACTIVE) {
            appUser.setStatus(AppUserStatus.INVITED);
            appUser.setActivatedAt(null);
            appUser.setSupabaseUserId(null);
        }

        AppUser saved = appUserRepository.save(appUser);
        log.info("Invite saved. email='{}', status={}, role={}", saved.getEmail(), saved.getStatus(), saved.getRole());
        return toResponse(saved);
    }

    public List<AppUserResponse> getUsers() {
        log.info("Fetching app user access list.");
        return appUserRepository.findAllByIsDeletedFalseOrderByEmailAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CurrentUserResponse getCurrentUser(Authentication authentication) {
        AppUser appUser = requireAppUser(authentication);
        log.info("Resolving current authenticated user. email='{}', role={}", appUser.getEmail(), appUser.getRole());
        return new CurrentUserResponse(appUser.getEmail(), appUser.getRole(), appUser.getStatus());
    }

    @Transactional
    public void bootstrapAdminIfConfigured() {
        String email = normalizeOptionalEmail(authProperties.getBootstrapAdmin().getEmail());
        String password = authProperties.getBootstrapAdmin().getPassword();

        if (email == null || password == null || password.isBlank()) {
            log.info("Bootstrap admin credentials are not configured. Skipping bootstrap admin initialization.");
            return;
        }

        if (appUserRepository.existsByRoleAndStatusAndIsDeletedFalse(AppUserRole.ADMIN, AppUserStatus.ACTIVE)) {
            log.info("Active admin already exists. Skipping bootstrap admin initialization.");
            return;
        }

        log.info("Bootstrapping initial admin user. email='{}'", email);
        AppUser appUser = appUserRepository.findByEmailIgnoreCaseAndIsDeletedFalse(email)
                .orElseGet(AppUser::new);
        appUser.setEmail(email);
        appUser.setRole(AppUserRole.ADMIN);
        appUser.setStatus(AppUserStatus.ACTIVE);
        if (appUser.getInvitedAt() == null) {
            appUser.setInvitedAt(LocalDateTime.now());
        }
        appUser.setActivatedAt(LocalDateTime.now());

        try {
            SupabaseAdminClient.SupabaseAdminUser createdUser = supabaseAdminClient.createUser(email, password, AppUserRole.ADMIN);
            appUser.setSupabaseUserId(createdUser.id());
        } catch (BusinessValidationException ex) {
            log.warn("Bootstrap admin Supabase create returned business warning. email='{}', message='{}'", email, ex.getMessage());
        }

        appUserRepository.save(appUser);
        log.info("Bootstrap admin initialization finished. email='{}'", email);
    }

    public AppUser requireAppUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResourceNotFoundException("AUTHENTICATION_CONTEXT_MISSING", "Authenticated user context is unavailable");
        }

        if (authentication.getPrincipal() instanceof AppUser appUser) {
            if (appUser.getStatus() != AppUserStatus.ACTIVE) {
                throw new BusinessValidationException("APP_USER_NOT_ACTIVE", "This user is not active for application access", java.util.Map.of("email", appUser.getEmail()));
            }
            return appUser;
        }

        String email = normalizeOptionalEmail(authentication.getName());
        if (email == null) {
            throw new ResourceNotFoundException("AUTHENTICATION_EMAIL_MISSING", "Email is missing in the authenticated principal");
        }

        AppUser appUser = appUserRepository.findByEmailIgnoreCaseAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("APP_USER_NOT_ALLOWED", "This user is not allowed to access the application"));

        if (appUser.getStatus() != AppUserStatus.ACTIVE) {
            throw new BusinessValidationException("APP_USER_NOT_ACTIVE", "This user is not active for application access", java.util.Map.of("email", email));
        }
        return appUser;
    }

    private AppUserResponse toResponse(AppUser appUser) {
        return new AppUserResponse(
                appUser.getId(),
                appUser.getEmail(),
                appUser.getRole(),
                appUser.getStatus(),
                appUser.getSupabaseUserId(),
                appUser.getInvitedAt(),
                appUser.getActivatedAt(),
                appUser.getCreatedAt(),
                appUser.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalEmail(String email) {
        String normalized = normalizeEmail(email);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
