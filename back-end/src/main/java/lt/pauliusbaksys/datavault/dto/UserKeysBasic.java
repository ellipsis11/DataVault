package lt.pauliusbaksys.datavault.dto;

public record UserKeysBasic(
        String kdfSaltB64,
        KdfParams kdfParams,
        String wrappedMasterKeyB64,
        String nonceB64,
        int vaultVersion,
        KdfContexts kdfContexts
) {}
