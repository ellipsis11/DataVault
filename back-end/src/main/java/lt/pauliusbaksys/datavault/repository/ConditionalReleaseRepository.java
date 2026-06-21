package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.enums.PolicyType;
import lt.pauliusbaksys.datavault.model.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConditionalReleaseRepository extends JpaRepository<Policy, UUID> {
    long countByOwner_Id(UUID ownerUserId);
    Optional<Policy> findByIdAndOwner_Id(UUID policyId, UUID ownerUserId);
    List<Policy> findAllByStatusIn(List<PolicyStatus> statuses);
    List<Policy> findAllByOwner_IdAndTypeAndStatusIn(UUID ownerId, PolicyType type, List<PolicyStatus> statuses);
    Page<Policy> findAllByOwner_IdAndPolicyNameContainingIgnoreCase(UUID userId, String query, Pageable pageable);
    Page<Policy> findAllByOwner_IdAndPolicyNameContainingIgnoreCaseAndStatus(
            UUID ownerId,
            String query,
            PolicyStatus status,
            Pageable pageable
    );
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OffsetDateTime from,
            OffsetDateTime to
    );
}
