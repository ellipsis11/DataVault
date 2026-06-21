package lt.pauliusbaksys.datavault.telegram;
import lt.pauliusbaksys.datavault.dto.TelegramLinkCodeDto;
import lt.pauliusbaksys.datavault.dto.TelegramLinkStatusDto;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.model.TelegramLinkCode;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.model.UserTelegramLink;
import lt.pauliusbaksys.datavault.repository.TelegramLinkCodeRepository;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import lt.pauliusbaksys.datavault.repository.UserTelegramLinkRepository;
import lt.pauliusbaksys.datavault.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class TelegramLinkService {

    private final TelegramLinkCodeRepository telegramLinkCodeRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserTelegramLinkRepository userTelegramLinkRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final AuditLogService auditLogService;

    public TelegramLinkService(TelegramLinkCodeRepository telegramLinkCodeRepository, UserRepository userRepository, UserTelegramLinkRepository userTelegramLinkRepository, TelegramNotificationService telegramNotificationService, AuditLogService auditLogService) {
        this.telegramLinkCodeRepository = telegramLinkCodeRepository;
        this.userRepository = userRepository;
        this.userTelegramLinkRepository = userTelegramLinkRepository;
        this.telegramNotificationService = telegramNotificationService;
        this.auditLogService = auditLogService;
    }

    public TelegramLinkCodeDto createLinkCode(UUID userId){
        try {
            String code = generateUniqueCode();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(2);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas!"));

            TelegramLinkCode linkCode = new TelegramLinkCode();
            linkCode.setCode(code);
            linkCode.setUser(user);
            linkCode.setExpiresAt(expiresAt);
            linkCode.setUsedAt(null);

            telegramLinkCodeRepository.save(linkCode);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.TELEGRAM_LINK,
                    "Telegram susiejimo kodas sugeneruotas",
                    Map.of("expiresAt",expiresAt.toString())
            );
            return new TelegramLinkCodeDto(code, expiresAt);
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.TELEGRAM_LINK,
                    "Nepavyko sugeneruoti Telegram susiejimo kodo",
                    Map.of(
                            "reason", e.getClass().getSimpleName(),
                            "errorMessage", e.getMessage()
                    )
            );
            throw e;
        }
    }

    // Generating a new code repeatedly until it does not already exist in the database.
    private String generateUniqueCode(){
        String code;

        do {
            code = generateCode();
        }
        while (telegramLinkCodeRepository.existsById(code));

        return code;
    }

    /**
     * Generating a secure 6-digit link code from 100000 to 999999.<br>
     * Kept as String because we do not perform calculations with it,
     * and Telegram command text is also received as String.
     */
    private String generateCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    @Transactional
    public void linkTelegramChat(String code, Long chatId, String username) {
        TelegramLinkCode linkCode = telegramLinkCodeRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Netinkamas susiejimo kodas!"));

        if (linkCode.isUsed()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Šis susiejimo kodas jau panaudotas!");
        }

        if (linkCode.isExpired()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Šio susiejimo kodo galiojimo laikas baigėsi!");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = linkCode.getUser();

        // Protecting DataVault account from linking twice
        if (userTelegramLinkRepository.existsById(user.getId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telegram paskyra jau susieta. Pirmiausia atsiekite ją!");
        }

        // Protecting Telegram account from being linked to multiple users
        if (userTelegramLinkRepository.existsByTelegramChatId(chatId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ši Telegram paskyra jau susieta!"
            );
        }

        UserTelegramLink userTelegramLink = new UserTelegramLink();
        userTelegramLink.setUser(user);
        userTelegramLink.setTelegramChatId(chatId);
        userTelegramLink.setTelegramUsername(username);
        userTelegramLink.setConnectedAt(now);

        userTelegramLinkRepository.save(userTelegramLink);

        linkCode.setUsedAt(now);
        telegramLinkCodeRepository.save(linkCode);
    }

    public TelegramLinkStatusDto getTelegramLinkStatus(UUID userId) {
        return userTelegramLinkRepository.findById(userId)
                .map(link -> new TelegramLinkStatusDto(
                        true,
                        link.getTelegramUsername(),
                        link.getConnectedAt(),
                        String.valueOf(link.getTelegramChatId())
                ))
                .orElse(new TelegramLinkStatusDto(
                        false,
                        null,
                        null,
                        null
                ));
    }

    // Not using isExpired() or isUsed() because we are deleting all the codes.
    @Transactional
    public void deleteExpiredUnusedCodes(){
        telegramLinkCodeRepository.deleteByExpiresAtBeforeAndUsedAtIsNull(OffsetDateTime.now());
    }

    public void sendTestMessageToTelegram(UUID userId) {
        UserTelegramLink userTelegramLink = userTelegramLinkRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Telegram paskyra nėra susieta!"
        ));

        telegramNotificationService.sendText(userTelegramLink.getTelegramChatId(),"✅ DataVault bandomasis pranešimas\nJūsų Telegram pranešimai veikia tinkamai.");
    }

    @Transactional
    public void unlinkTelegramAccount(UUID userId) {
        try {
            UserTelegramLink userTelegramLink = userTelegramLinkRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Telegram paskyra nerasta!"
                    ));
            userTelegramLinkRepository.delete(userTelegramLink);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.TELEGRAM_UNLINK,
                    "Telegram paskyra sėkmingai atsieta"
            );
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.TELEGRAM_UNLINK,
                    "Nepavyko atsieti Telegram paskyros",
                    Map.of(
                            "reason", e.getClass().getSimpleName(),
                            "errorMessage", e.getMessage()
                    )
            );
            throw e;
        }
    }
}