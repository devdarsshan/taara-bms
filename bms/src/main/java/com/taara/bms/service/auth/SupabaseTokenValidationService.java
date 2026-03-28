package com.taara.bms.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taara.bms.config.AuthProperties;
import com.taara.bms.entity.auth.AppUser;
import com.taara.bms.enums.AppUserStatus;
import com.taara.bms.repo.auth.AppUserRepository;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SupabaseTokenValidationService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseTokenValidationService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppUserRepository appUserRepository;

    public SupabaseTokenValidationService(AuthProperties authProperties, AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
        this.restClient = RestClient.builder()
                .baseUrl(authProperties.getSupabase().getUrl())
                .defaultHeader("apikey", authProperties.getSupabase().getAnonKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AppUser validateAccessToken(String accessToken) {
        String email = fetchEmailFromSupabase(accessToken);
        AppUser appUser = appUserRepository.findByEmailIgnoreCaseAndIsDeletedFalse(email)
                .orElseThrow(() -> {
                    log.warn("Supabase token validated, but email is not allowlisted. email='{}'", email);
                    return new InsufficientAuthenticationException("This user is not allowed to access the application");
                });

        if (appUser.getStatus() != AppUserStatus.ACTIVE) {
            log.warn("Supabase token validated, but app user is not active. email='{}', status={}", email, appUser.getStatus());
            throw new InsufficientAuthenticationException("This user is not active for application access");
        }

        log.info("Supabase token validated successfully. email='{}', role={}", appUser.getEmail(), appUser.getRole());
        return appUser;
    }

    private String fetchEmailFromSupabase(String accessToken) {
        try {
            String response = restClient.get()
                    .uri("/auth/v1/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            String email = normalize(root.path("email").asText(null));
            if (email == null) {
                log.warn("Supabase /auth/v1/user response did not contain an email.");
                throw new BadCredentialsException("Supabase token response did not include an email");
            }
            return email;
        } catch (RestClientResponseException ex) {
            String message = extractErrorMessage(ex.getResponseBodyAsString());
            log.warn("Supabase token validation failed. status={}, message='{}'", ex.getStatusCode(), message);
            throw new BadCredentialsException(message);
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while validating Supabase access token.", ex);
            throw new BadCredentialsException("Unable to validate Supabase access token");
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            if (root.hasNonNull("msg")) {
                return root.path("msg").asText();
            }
            if (root.hasNonNull("message")) {
                return root.path("message").asText();
            }
            if (root.hasNonNull("error_description")) {
                return root.path("error_description").asText();
            }
        } catch (Exception ex) {
            log.debug("Unable to parse Supabase token validation error body.", ex);
        }
        return "Supabase rejected the access token";
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
