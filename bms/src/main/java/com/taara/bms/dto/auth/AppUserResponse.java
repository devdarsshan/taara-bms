package com.taara.bms.dto.auth;

import com.taara.bms.enums.AppUserRole;
import com.taara.bms.enums.AppUserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppUserResponse(
        UUID id,
        String email,
        AppUserRole role,
        AppUserStatus status,
        String supabaseUserId,
        LocalDateTime invitedAt,
        LocalDateTime activatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
