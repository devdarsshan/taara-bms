package com.taara.bms.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taara.bms.config.AuthProperties;
import com.taara.bms.enums.AppUserRole;
import com.taara.bms.exception.BusinessValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SupabaseAdminClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAdminClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseAdminClient(AuthProperties authProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(authProperties.getSupabase().getUrl())
                .defaultHeader("apikey", authProperties.getSupabase().getServiceRoleKey())
                .defaultHeader("Authorization", "Bearer " + authProperties.getSupabase().getServiceRoleKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public SupabaseAdminUser createUser(String email, String password, AppUserRole role) {
        log.info("Creating Supabase auth user. email='{}', role={}", email, role);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("email_confirm", true);
        payload.put("app_metadata", Map.of("user_role", role.name()));

        try {
            String response = restClient.post()
                    .uri("/auth/v1/admin/users")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            JsonNode userNode = root.hasNonNull("user") ? root.path("user") : root;
            String userId = userNode.path("id").asText(null);
            String userEmail = userNode.path("email").asText(email);
            log.info("Supabase auth user create succeeded. email='{}', userId='{}'", userEmail, userId);
            return new SupabaseAdminUser(userId, userEmail);
        } catch (RestClientResponseException ex) {
            String message = extractErrorMessage(ex.getResponseBodyAsString());
            log.warn("Supabase auth user create failed. email='{}', status={}, message='{}'", email, ex.getStatusCode(), message);
            throw new BusinessValidationException("SUPABASE_USER_CREATE_FAILED", message, Map.of("email", email));
        } catch (Exception ex) {
            log.error("Unexpected error while creating Supabase auth user. email='{}'", email, ex);
            throw new BusinessValidationException(
                    "SUPABASE_USER_CREATE_FAILED",
                    "Unable to create auth user in Supabase",
                    Map.of("email", email)
            );
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
            log.debug("Unable to parse Supabase error body.", ex);
        }
        return "Unable to create auth user in Supabase";
    }

    public record SupabaseAdminUser(String id, String email) {
    }
}
