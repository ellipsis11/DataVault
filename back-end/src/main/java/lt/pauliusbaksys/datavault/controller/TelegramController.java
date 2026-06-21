package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.TelegramLinkCodeDto;
import lt.pauliusbaksys.datavault.dto.TelegramLinkStatusDto;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.telegram.TelegramLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private final TelegramLinkService telegramLinkService;
    private final CurrentUserService currentUserService;

    public TelegramController(TelegramLinkService telegramLinkService, CurrentUserService currentUserService) {
        this.telegramLinkService = telegramLinkService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/link")
    public TelegramLinkCodeDto createTelegramLinkCode(HttpServletRequest req){
        return telegramLinkService.createLinkCode(currentUserService.getCurrentUserId(req));
    }

    @GetMapping("/link-status")
    public TelegramLinkStatusDto getTelegramLinkStatus(HttpServletRequest req){
        return telegramLinkService.getTelegramLinkStatus(currentUserService.getCurrentUserId(req));
    }

    @PostMapping("/test-message")
    public ResponseEntity<?> sendTestMessage(HttpServletRequest req){
        telegramLinkService.sendTestMessageToTelegram(currentUserService.getCurrentUserId(req));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/link")
    public ResponseEntity<?> unlinkTelegramAccount(HttpServletRequest req) {
        telegramLinkService.unlinkTelegramAccount(currentUserService.getCurrentUserId(req));
        return ResponseEntity.noContent().build();
    }
}
