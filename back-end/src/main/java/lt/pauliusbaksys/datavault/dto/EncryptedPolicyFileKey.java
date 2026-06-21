package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record EncryptedPolicyFileKey(
        UUID fileId,
        String fileKeyEncryptedB64,
        String fileKeyNonceB64,
        String metaKeyEncryptedB64,
        String metaKeyNonceB64
) {}
