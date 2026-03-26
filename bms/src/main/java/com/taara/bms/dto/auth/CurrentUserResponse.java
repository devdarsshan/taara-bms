package com.taara.bms.dto.auth;

import com.taara.bms.enums.AppUserRole;
import com.taara.bms.enums.AppUserStatus;

public record CurrentUserResponse(
        String email,
        AppUserRole role,
        AppUserStatus status
) {
}
