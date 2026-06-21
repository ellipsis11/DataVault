package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record RecipientPublicKey(
        UUID recipientUserId,
        String publicKeyB64
) {}
