package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.model.UserTelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTelegramLinkRepository extends JpaRepository<UserTelegramLink, UUID> {
    boolean existsByTelegramChatId(Long telegramChatId);
}
