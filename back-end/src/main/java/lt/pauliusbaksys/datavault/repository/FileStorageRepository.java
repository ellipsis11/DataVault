package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.model.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileStorageRepository extends JpaRepository<File, UUID> {
    long countByOwner_Id(UUID ownerUserId);
    Page<File> findAllByOwner_Id(UUID ownerUserId, Pageable pageable);
    List<File> findAllByOwner_Id(UUID ownerUserId);
    Optional<File> findByIdAndOwner_Id(UUID fileId, UUID ownerUserId);
    List<File> findAllByIdInAndOwner_Id(List<UUID> fileIds, UUID ownerUserId);
    List<File> findAllByOwner_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID ownerId,
            OffsetDateTime from,
            OffsetDateTime to
    );
    boolean existsByOwnerIdAndContentHash(UUID ownerId, byte[] hash);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OffsetDateTime from,
            OffsetDateTime to
    );
}