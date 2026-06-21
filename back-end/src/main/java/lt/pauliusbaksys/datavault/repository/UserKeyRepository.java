package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.model.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserKeyRepository extends JpaRepository<UserKey, UUID> {

    boolean existsByUserId(UUID uuid);
    Optional<UserKey> findUserKeyByUserId(UUID userId);
    List<UserKey> findAllByUserIdIn(List<UUID> userIds);
}
