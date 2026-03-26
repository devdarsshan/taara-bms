package com.taara.bms.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private final Supabase supabase = new Supabase();
    private final Jwt jwt = new Jwt();
    private final BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    public Supabase getSupabase() {
        return supabase;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public static class Supabase {
        @NotBlank
        private String url;

        @NotBlank
        private String anonKey;

        @NotBlank
        private String serviceRoleKey;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAnonKey() {
            return anonKey;
        }

        public void setAnonKey(String anonKey) {
            this.anonKey = anonKey;
        }

        public String getServiceRoleKey() {
            return serviceRoleKey;
        }

        public void setServiceRoleKey(String serviceRoleKey) {
            this.serviceRoleKey = serviceRoleKey;
        }
    }

    public static class Jwt {
        @NotBlank
        private String issuer;

        @NotBlank
        private String jwkSetUri;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }
    }

    public static class BootstrapAdmin {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
