package lt.pauliusbaksys.datavault.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_telegram_links")
public class UserTelegramLink {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "connected_at", nullable = false)
    private OffsetDateTime connectedAt;

    public UserTelegramLink() {
    }

    public UserTelegramLink(User user, Long telegramChatId, String telegramUsername) {
        this.user = user;
        this.telegramChatId = telegramChatId;
        this.telegramUsername = telegramUsername;
    }

    public User getUser() {
        return user;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public OffsetDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public void setConnectedAt(OffsetDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
}