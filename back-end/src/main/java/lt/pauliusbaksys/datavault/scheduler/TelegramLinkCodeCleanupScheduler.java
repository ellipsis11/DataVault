package lt.pauliusbaksys.datavault.scheduler;

import lt.pauliusbaksys.datavault.telegram.TelegramLinkService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TelegramLinkCodeCleanupScheduler {

    private final TelegramLinkService telegramLinkService;

    public TelegramLinkCodeCleanupScheduler(TelegramLinkService telegramLinkService) {
        this.telegramLinkService = telegramLinkService;
    }

    // Public method because scheduler needs to see it.
    @Scheduled(fixedRate = 60_000)
    public void cleanupExpiredCodes(){
        telegramLinkService.deleteExpiredUnusedCodes();
    }
}
