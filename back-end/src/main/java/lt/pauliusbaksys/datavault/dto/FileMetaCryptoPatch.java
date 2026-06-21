package lt.pauliusbaksys.datavault.dto;

public record FileMetaCryptoPatch(
        String metaNonceB64,
        String metaCipherB64
) {}
