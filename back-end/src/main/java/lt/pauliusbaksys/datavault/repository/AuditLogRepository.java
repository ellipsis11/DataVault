package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    long countByUser_Id(UUID userId);
    Page<AuditLog> findAllByUser_Id(UUID userId, Pageable pageable);
    List<AuditLog> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
    List<AuditLog> findAllByUser_IdOrderByCreatedAtAsc(UUID userId);
    List<AuditLog> findTop10ByUser_IdOrderByCreatedAtDesc(UUID userId);
    // Using native SQL because of the user_id casting
    @Query(value = """
        SELECT *
        FROM audit_logs a
        WHERE (
           LOWER(COALESCE(a.actor_email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           OR CAST(a.user_id AS TEXT) LIKE CONCAT('%', :query, '%')
           OR LOWER(COALESCE(a.message, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND a.action_type IN (:actions)
        AND (:userId IS NULL OR a.user_id = :userId)
        AND a.created_at >= :from
        AND a.created_at < :to
        ORDER BY a.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM audit_logs a
        WHERE (
           LOWER(COALESCE(a.actor_email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           OR CAST(a.user_id AS TEXT) LIKE CONCAT('%', :query, '%')
           OR LOWER(COALESCE(a.message, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND a.action_type IN (:actions)
        AND (:userId IS NULL OR a.user_id = :userId)
        AND a.created_at >= :from
        AND a.created_at < :to
        """,
        nativeQuery = true)
    Page<AuditLog> searchAdminAuditLogs(
            @Param("query") String query,
            @Param("actions") Collection<String> actions,
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );
    // Because this is a JPQL we are using enum AuditAction
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.user.id = :userId
            AND LOWER(COALESCE(a.message, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            AND a.actionType IN (:actions)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> searchAuditLogs(
            @Param("userId")UUID userId,
            @Param("query")String query,
            @Param("actions")Collection<AuditAction> actions,
            Pageable pageable
    );
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
    Optional<AuditLog> findTopByOrderByCreatedAtDesc();
}
