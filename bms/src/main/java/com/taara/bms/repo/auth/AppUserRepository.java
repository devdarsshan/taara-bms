package com.taara.bms.repo.auth;

import com.taara.bms.entity.auth.AppUser;
import com.taara.bms.enums.AppUserRole;
import com.taara.bms.enums.AppUserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    boolean existsByRoleAndStatusAndIsDeletedFalse(AppUserRole role, AppUserStatus status);

    List<AppUser> findAllByIsDeletedFalseOrderByEmailAsc();
}
