package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record RecipientValidationItem(
        UUID recipientId,
        boolean telegramLinked,
        boolean keysGenerated
) {}