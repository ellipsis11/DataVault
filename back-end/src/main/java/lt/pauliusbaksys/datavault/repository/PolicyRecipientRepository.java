package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.model.PolicyRecipientId;
import lt.pauliusbaksys.datavault.model.PolicyRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PolicyRecipientRepository extends JpaRepository<PolicyRecipient, PolicyRecipientId> {
    long countByRecipient_Id(UUID recipientId);
    Optional<PolicyRecipient> findByPolicy_IdAndRecipient_Id(UUID policyId, UUID recipientId);
    List<PolicyRecipient> findAllByPolicy_Id(UUID policyId);
    void deleteAllByPolicy_Id(UUID policyId);
    List<PolicyRecipient> findAllByRecipient_Id(UUID recipientId);
    long countByRecipient_IdAndPolicy_Status(
            UUID recipientId,
            PolicyStatus status
    );
    int countByPolicy_Id(UUID policyId);
    // Using JPQL we dont need COUNT
    @Query("""
            SELECT pr
            FROM PolicyRecipient pr
            WHERE pr.recipient.id = :recipientId
                AND pr.policy.status = :policyStatus
                AND LOWER(pr.policy.policyName) LIKE LOWER(CONCAT('%', :query, '%'))
                AND (
                        :viewed IS NULL
                        OR (:viewed = false AND pr.accessCount = 0)
                        OR (:viewed = true AND pr.accessCount > 0)
                    )
            """)
    Page<PolicyRecipient> searchViewedReceivedPolicies(
            @Param("recipientId") UUID recipientId,
            @Param("policyStatus") PolicyStatus policyStatus,
            @Param("query") String query,
            @Param("viewed") Boolean viewed,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(pr)
        FROM PolicyRecipient pr
        WHERE pr.recipient.id = :recipientId
            AND pr.policy.status = :policyStatus
            AND pr.accessCount = 0
    """)
    long countNewReleasedPolicies(
            @Param("recipientId") UUID recipientId,
            @Param("policyStatus") PolicyStatus policyStatus
    );
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OffsetDateTime from,
            OffsetDateTime to
    );
}