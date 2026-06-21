package lt.pauliusbaksys.datavault.dto;
import lt.pauliusbaksys.datavault.enums.StorageType;
import java.util.UUID;

public record FileMetaFull(
        UUID id,
        StorageType storageType,
        String storageRef,
        String subKeyId,
        String metaNonceB64,
        String metaCipherB64
        ) {}
