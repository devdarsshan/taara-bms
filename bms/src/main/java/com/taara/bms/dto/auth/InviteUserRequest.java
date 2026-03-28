package com.taara.bms.dto.auth;

import com.taara.bms.enums.AppUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteUserRequest(
        @NotBlank @Email String email,
        @NotNull AppUserRole role
) {
}
