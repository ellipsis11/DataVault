package lt.pauliusbaksys.datavault.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyRecipientDto(
        UUID id,
        String email,
        int totalAccesses,
        OffsetDateTime firstAccessedAt,
        OffsetDateTime lastAccessedAt
) {}
