package com.taara.bms.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final String frontendStaticPath;

    public WebConfig(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins,
            @Value("${app.frontend.static-path:}") String frontendStaticPath
    ) {
        this.allowedOrigins = allowedOrigins;
        this.frontendStaticPath = frontendStaticPath;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        List<String> resourceLocations = new ArrayList<>();
        if (StringUtils.hasText(frontendStaticPath)) {
            resourceLocations.add(asResourceLocation(frontendStaticPath));
        }
        resourceLocations.add("classpath:/static/");

        registry.addResourceHandler("/**")
                .addResourceLocations(resourceLocations.toArray(String[]::new))
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private String asResourceLocation(String path) {
        String normalizedPath = path.endsWith("/") ? path : path + "/";
        if (normalizedPath.startsWith("classpath:") || normalizedPath.startsWith("file:")) {
            return normalizedPath;
        }
        return "file:" + normalizedPath;
    }

    private static final class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            if (isApiOrConfigPath(resourcePath)) {
                return null;
            }

            if (isStaticAssetRequest(resourcePath)) {
                Resource requestedResource = location.createRelative(resourcePath);
                if (requestedResource.exists() && requestedResource.isReadable()) {
                    return requestedResource;
                }
                return null;
            }

            Resource indexResource = location.createRelative("index.html");
            if (indexResource.exists() && indexResource.isReadable()) {
                return indexResource;
            }

            return null;
        }

        private boolean isApiOrConfigPath(String resourcePath) {
            return "api".equals(resourcePath)
                    || resourcePath.startsWith("api/")
                    || "app-config.js".equals(resourcePath);
        }

        private boolean isStaticAssetRequest(String resourcePath) {
            return StringUtils.hasText(resourcePath) && resourcePath.contains(".");
        }
    }
}
