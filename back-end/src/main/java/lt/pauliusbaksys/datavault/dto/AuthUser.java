package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.UserRole;

import java.util.UUID;

public record AuthUser(
        UUID userId,
        String email,
        UserRole role
) {}
