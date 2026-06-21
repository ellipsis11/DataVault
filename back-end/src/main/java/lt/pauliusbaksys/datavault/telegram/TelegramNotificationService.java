package lt.pauliusbaksys.datavault.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

@Service
public class TelegramNotificationService {

    private final TelegramClient telegramClient;
    private final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    /**
     * Creating a Telegram client using bot token.
     *<br>
     * <ul>
     * <li>OkHttpTelegramClient is the Telegram client implementation that uses
     * OkHttp internally to send HTTP requests to the Telegram Bot API.</li>
     * </ul>
     */
    public TelegramNotificationService(@Value("${telegram.bot.token}") String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    public void sendText(Long chatId, String text) {
        try{
            telegramClient.execute(
                    SendMessage.builder()
                            .chatId(chatId.toString())
                            .text(text)
                            .build()
            );
        }
        catch (TelegramApiException e){
            log.error("Nepavyko išsiųsti Telegram pranešimo į pokalbį={}", chatId, e);
        }
    }
}
