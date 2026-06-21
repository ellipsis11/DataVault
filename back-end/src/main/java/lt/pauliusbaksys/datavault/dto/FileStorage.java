package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.StorageType;

import java.util.UUID;

//Prevent SQL injection maybe add constraints to all DTO's
public record FileStorage(
        UUID fileId,
        String subKeyId,
        String contentHashB64,
        String metaNonceB64,
        String metaCipherB64,
        StorageType storageType
) {
    public long subKeyIdAsLong(){
        return Long.parseLong(subKeyId);
    }
}
