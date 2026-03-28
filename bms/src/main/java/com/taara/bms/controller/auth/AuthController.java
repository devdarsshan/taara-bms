package com.taara.bms.controller.auth;

import com.taara.bms.dto.auth.AppUserResponse;
import com.taara.bms.dto.auth.CurrentUserResponse;
import com.taara.bms.dto.auth.InviteUserRequest;
import com.taara.bms.dto.auth.SignupRequest;
import com.taara.bms.dto.common.MessageResponse;
import com.taara.bms.service.auth.AppAuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AppAuthService appAuthService;

    public AuthController(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    @PostMapping("/signup")
    public MessageResponse signup(@Valid @RequestBody SignupRequest request) {
        log.info("Received invited signup request. email='{}'", request.email());
        return appAuthService.signup(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(Authentication authentication) {
        log.info("Fetching current authenticated user profile.");
        return appAuthService.getCurrentUser(authentication);
    }

    @GetMapping("/admin/users")
    public List<AppUserResponse> getUsers() {
        log.info("Fetching access-managed users.");
        return appAuthService.getUsers();
    }

    @PostMapping("/admin/users")
    public AppUserResponse inviteUser(@Valid @RequestBody InviteUserRequest request) {
        log.info("Creating invited access-managed user. email='{}', role={}", request.email(), request.role());
        return appAuthService.inviteUser(request);
    }
}
