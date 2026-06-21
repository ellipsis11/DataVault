package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.StorageType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FileMetaSummary(UUID id, StorageType storageType, OffsetDateTime createdAt) {}
