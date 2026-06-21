package lt.pauliusbaksys.datavault.model;
import jakarta.persistence.*;
import lt.pauliusbaksys.datavault.enums.StorageType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "files",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"owner_user_id", "subkey_id"}),
                @UniqueConstraint(columnNames = {"owner_user_id", "content_hash"})
        }
)
public class File {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 16, nullable = false)
    private StorageType storageType;

    @Column(name = "storage_ref", nullable = false)
    private String storageRef;

    @Column(name = "subkey_id", nullable = false)
    private Long subKeyId;

    @Column(name = "content_hash", nullable = false, columnDefinition = "bytea")
    private byte[] contentHash;

    @Column(name = "meta_nonce", nullable = false, columnDefinition = "bytea")
    private byte[] metaNonce;

    @Column(name = "meta_cipher", nullable = false, columnDefinition = "bytea")
    private byte[] metaCipher;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public File() {
    }

    public File(UUID id, User owner, StorageType storageType, String storageRef, Long subKeyId, byte[] contentHash, byte[] metaNonce, byte[] metaCipher) {
        this.id = id;
        this.owner = owner;
        this.storageType = storageType;
        this.storageRef = storageRef;
        this.subKeyId = subKeyId;
        this.contentHash = contentHash;
        this.metaNonce = metaNonce;
        this.metaCipher = metaCipher;
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public Long getSubKeyId() {
        return subKeyId;
    }

    public byte[] getContentHash() {
        return contentHash;
    }

    public byte[] getMetaNonce() {
        return metaNonce;
    }

    public byte[] getMetaCipher() {
        return metaCipher;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setMetaNonce(byte[] metaNonce) {
        this.metaNonce = metaNonce;
    }

    public void setMetaCipher(byte[] metaCipher) {
        this.metaCipher = metaCipher;
    }
}
