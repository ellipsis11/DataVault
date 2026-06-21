package lt.pauliusbaksys.datavault.telegram;

import lt.pauliusbaksys.datavault.model.Policy;
import lt.pauliusbaksys.datavault.model.PolicyRecipient;
import lt.pauliusbaksys.datavault.repository.PolicyRecipientRepository;
import lt.pauliusbaksys.datavault.repository.UserTelegramLinkRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyNotificationService {

    private final TelegramNotificationService telegramNotificationService;
    private final UserTelegramLinkRepository userTelegramLinkRepository;
    private final PolicyRecipientRepository policyRecipientRepository;

    private static String formatDateTime(OffsetDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public PolicyNotificationService(TelegramNotificationService telegramNotificationService, UserTelegramLinkRepository userTelegramLinkRepository, PolicyRecipientRepository policyRecipientRepository) {
        this.telegramNotificationService = telegramNotificationService;
        this.userTelegramLinkRepository = userTelegramLinkRepository;
        this.policyRecipientRepository = policyRecipientRepository;
    }

    public void sendDateTimeWarning(Policy policy) {
        sendToOwner(policy,
                "⚠️ DataVault įspėjimas\n" +
                        "Politika \"" + policy.getPolicyName() + "\" netrukus bus atskleista.\n" +
                        "Atskleidimo laikas: " + formatDateTime(policy.getScheduledReleaseAt()));
    }

    public void sendGraceStarted(Policy policy, OffsetDateTime releaseDeadline) {
        sendToOwner(policy,
                "⚠️ DataVault neveiklumo įspėjimas\n" +
                        "Politika \"" + policy.getPolicyName() + "\" perėjo į atidėjimo laikotarpį.\n" +
                        "Atidėjimo laikotarpis: " + policy.getGraceDays() + " d.\n" +
                        "Atskleidimas, jei aktyvumas nebus patvirtintas iki: " + formatDateTime(releaseDeadline));
    }

    public void sendGraceWarning(Policy policy, OffsetDateTime releaseDeadline) {
        sendToOwner(policy,
                "⚠️ DataVault priminimas\n" +
                        "Politika \"" + policy.getPolicyName() + "\" vis dar yra atidėjimo laikotarpyje.\n" +
                        "Bus atskleista, jei aktyvumas nebus patvirtintas iki: " + formatDateTime(releaseDeadline));
    }

    public void sendReleased(Policy policy) {
        sendToOwner(policy,
                "✅ DataVault atskleidimas\n" +
                        "Politika \"" + policy.getPolicyName() + "\" buvo atskleista."
        );

        List<PolicyRecipient> recipients = policyRecipientRepository.findAllByPolicy_Id(policy.getId());
        List<String> recipientsWithoutTelegram = new ArrayList<>();

        for (PolicyRecipient recipient : recipients){
            userTelegramLinkRepository.findById(recipient.getRecipient().getId())
                    .ifPresentOrElse(link -> {
                        telegramNotificationService.sendText(
                                link.getTelegramChatId(),
                                "\uD83D\uDD13 DataVault prieiga atskleista\n" +
                                        "Jums suteikta prieiga pagal politiką \"" + policy.getPolicyName() + "\""
                                );
                    },
                    () -> recipientsWithoutTelegram.add(recipient.getRecipient().getEmail())
                    );
        }

        if (recipientsWithoutTelegram.size() == 1) {
            sendToOwner(policy,
                    "⚠️ DataVault pranešimo įspėjimas\n" +
                            "Politika buvo atskleista, bet gavėjas nėra susiejęs Telegram boto!\n" +
                            "Gavėjas: " + recipientsWithoutTelegram.getFirst()
            );
        }

        if (recipientsWithoutTelegram.size() > 1){
            sendToOwner(policy,
                    "⚠️ DataVault pranešimo įspėjimas\n" +
                            "Politika buvo atskleista, bet kai kurie gavėjai nėra susieję Telegram boto!\n" +
                            "Gavėjai:\n" +
                            String.join("\n", recipientsWithoutTelegram)
            );
        }
    }

    private void sendToOwner(Policy policy, String message){
        userTelegramLinkRepository.findById(policy.getOwner().getId())
                .ifPresent(link -> telegramNotificationService.sendText(link.getTelegramChatId(), message));
    }
}