package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserListItem(
        UUID id,
        String email,
        UserRole role,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        long logCount
) {}