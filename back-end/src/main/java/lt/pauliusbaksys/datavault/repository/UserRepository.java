package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findTop10ByEmailStartingWithIgnoreCase(String q);
    @Query(value = """
            SELECT *
            FROM users u
            WHERE LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR CAST(u.id AS TEXT) LIKE CONCAT('%', :query, '%')
            ORDER BY u.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM USERS u
            WHERE LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR CAST(u.id AS TEXT) LIKE CONCAT('%', :query, '%')
            """,
            nativeQuery = true
    )
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OffsetDateTime from,
            OffsetDateTime to
    );

}
