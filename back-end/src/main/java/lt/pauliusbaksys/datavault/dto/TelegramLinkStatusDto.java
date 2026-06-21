package lt.pauliusbaksys.datavault.dto;

import java.time.OffsetDateTime;

public record TelegramLinkStatusDto(
        boolean telegramLinked,
        String telegramUsername,
        OffsetDateTime connectedAt,
        String chatId
) {}