package lt.pauliusbaksys.datavault.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;


@Component
public class DatavaultTelegramBot implements SpringLongPollingBot {

    private final String botToken;
    private final String botUsername;
    private final TelegramLinkService telegramLinkService;
    private final TelegramNotificationService telegramNotificationService;
    private final Logger log = LoggerFactory.getLogger(DatavaultTelegramBot.class);

    public DatavaultTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            TelegramLinkService telegramLinkService,
            TelegramNotificationService telegramNotificationService
    ) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.telegramLinkService = telegramLinkService;
        this.telegramNotificationService = telegramNotificationService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Receiving Telegram updates using long polling and passing each update
     * to the update handler for further processing.
     *<br>
     * <ul>
     * <li>Updates come as a list of new Telegram events that the library receives via long polling.</li>
     * <li>For example, when a user writes a message to the bot, Telegram creates an Update, and we receive it here.</li>
     * </ul>
     * Long polling – application periodically asks Telegram
     * for new updates instead of waiting for Telegram to call our server (webhook method).
     */
    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
        };
    }

    private void handleUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()){
            return;
        }

        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(text)){
            telegramNotificationService.sendText(chatId, "Sveiki prisijungę prie DataVault boto!\nKomandos:\n/chat-id - parodyti jūsų Telegram pokalbio ID.");
            return;
        }

        if ("/chat-id".equals(text)){
            telegramNotificationService.sendText(chatId, "Jūsų pokalbio ID: " + chatId);
            return;
        }

        if (text.startsWith("/link ")){
            String code = text.substring(6).trim();
            String username = update.getMessage().getFrom() != null
                    ? update.getMessage().getFrom().getUserName()
                    : null;
            try {
                telegramLinkService.linkTelegramChat(code, chatId, username);
                telegramNotificationService.sendText(chatId, "✅ Telegram paskyra sėkmingai susieta.\nNuo šiol čia gausite DataVault pranešimus.");
            }
            catch (ResponseStatusException e) {
                telegramNotificationService.sendText(chatId, e.getReason() != null ? e.getReason() : "Nepavyko susieti Telegram paskyros!");
            } catch (Exception e) {
                telegramNotificationService.sendText(chatId, "Netikėta klaida susiejant Telegram paskyrą!");
            }
            return;
        }

        telegramNotificationService.sendText(chatId, "Nežinoma komanda.");
    }
}