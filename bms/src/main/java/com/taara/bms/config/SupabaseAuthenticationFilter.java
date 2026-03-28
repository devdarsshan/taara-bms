package com.taara.bms.config;

import com.taara.bms.entity.auth.AppUser;
import com.taara.bms.service.auth.SupabaseTokenValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Component
public class SupabaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SupabaseTokenValidationService tokenValidationService;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public SupabaseAuthenticationFilter(
            SupabaseTokenValidationService tokenValidationService,
            JsonAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.tokenValidationService = tokenValidationService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = authorization.substring(BEARER_PREFIX.length()).trim();
            AppUser appUser = tokenValidationService.validateAccessToken(accessToken);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    appUser,
                    accessToken,
                    List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("Supabase authentication filter rejected request '{} {}'. Reason: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            authenticationEntryPoint.commence(request, response, new BadCredentialsException(ex.getMessage(), ex));
        }
    }
}
