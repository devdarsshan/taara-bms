package com.taara.bms.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendConfigController {

    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String supabaseUrl;
    private final String supabaseAnonKey;

    public FrontendConfigController(
            ObjectMapper objectMapper,
            @Value("${app.frontend.api-base-url:/api}") String apiBaseUrl,
            @Value("${app.auth.supabase.url:}") String supabaseUrl,
            @Value("${app.auth.supabase.anon-key:}") String supabaseAnonKey
    ) {
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
    }

    @GetMapping(value = "/app-config.js", produces = "application/javascript")
    public ResponseEntity<String> appConfig() {
        String payload = """
                window.__TAARA_CONFIG__ = {
                  apiBaseUrl: %s,
                  supabaseUrl: %s,
                  supabaseAnonKey: %s
                };
                """.formatted(
                toJson(apiBaseUrl),
                toJson(supabaseUrl),
                toJson(supabaseAnonKey)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(new MediaType("application", "javascript"))
                .body(payload);
    }

    private String toJson(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to render frontend runtime config", ex);
        }
    }
}
