package lt.pauliusbaksys.datavault.model;

import jakarta.persistence.*;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id", nullable = true) // null for failed login etc.
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "audit_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditLevel auditLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private AuditAction actionType;

    @Column(nullable = false)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "previous_hash")
    private byte[] previousHash;

    @Column(name = "current_hash", nullable = false)
    private byte[] currentHash ;

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public AuditLevel getAuditLevel() {
        return auditLevel;
    }

    public void setAuditLevel(AuditLevel auditLevel) {
        this.auditLevel = auditLevel;
    }

    public AuditAction getActionType() {
        return actionType;
    }

    public void setActionType(AuditAction actionType) {
        this.actionType = actionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public byte[] getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(byte[] previousHash) {
        this.previousHash = previousHash;
    }

    public byte[] getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(byte[] currentHash) {
        this.currentHash = currentHash;
    }
}
