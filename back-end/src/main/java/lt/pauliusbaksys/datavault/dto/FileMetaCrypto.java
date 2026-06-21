package lt.pauliusbaksys.datavault.dto;

public record FileMetaCrypto(
        String subKeyId,
        String metaNonceB64,
        String metaCipherB64
) {}
