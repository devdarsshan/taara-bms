package com.taara.bms.config;

import com.taara.bms.entity.auth.AppUser;
import com.taara.bms.enums.AppUserStatus;
import com.taara.bms.repo.auth.AppUserRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtAuthenticationConverter.class);

    private final AppUserRepository appUserRepository;

    public SupabaseJwtAuthenticationConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        String email = normalize(jwt.getClaimAsString("email"));
        if (email == null) {
            log.warn("JWT rejected because email claim is missing. subject='{}'", jwt.getSubject());
            throw new InsufficientAuthenticationException("Email claim is missing in the access token");
        }

        AppUser appUser = appUserRepository.findByEmailIgnoreCaseAndIsDeletedFalse(email)
                .orElseThrow(() -> {
                    log.warn("JWT rejected because email is not allowlisted. email='{}'", email);
                    return new InsufficientAuthenticationException("This user is not allowed to access the application");
                });

        if (appUser.getStatus() != AppUserStatus.ACTIVE) {
            log.warn("JWT rejected because user is not active. email='{}', status={}", email, appUser.getStatus());
            throw new InsufficientAuthenticationException("This user is not active for application access");
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities, email);
        authentication.setDetails(appUser);
        log.info("Mapped JWT to application authorities. email='{}', role={}", email, appUser.getRole());
        return authentication;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
