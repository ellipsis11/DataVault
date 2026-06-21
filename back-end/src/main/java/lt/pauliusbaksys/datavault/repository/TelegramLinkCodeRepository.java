package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.model.TelegramLinkCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface TelegramLinkCodeRepository extends JpaRepository<TelegramLinkCode, String> {
    void deleteByExpiresAtBeforeAndUsedAtIsNull(OffsetDateTime now);
}
