package lt.pauliusbaksys.datavault.dto;

import java.time.OffsetDateTime;

public record TelegramLinkCodeDto(
        String code,
        OffsetDateTime expiresAt
) {}
