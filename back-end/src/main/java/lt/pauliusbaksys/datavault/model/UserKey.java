package lt.pauliusbaksys.datavault.model;
import jakarta.persistence.*;
import lt.pauliusbaksys.datavault.dto.KdfContexts;
import lt.pauliusbaksys.datavault.dto.KdfParams;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_keys")
public class UserKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false) // JPA level. No entity creation without this value
    @JoinColumn(name = "user_id", nullable = false, unique = true) // Not nullable – no value in this cell without FK
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "encrypted_master_key", nullable = false, columnDefinition = "bytea")
    private byte[] encryptedMasterKey;

    @Column(name = "kdf_salt", nullable = false, columnDefinition = "bytea")
    private byte[] kdfSalt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kdf_params", nullable = false, columnDefinition = "jsonb")
    private KdfParams kdfParams;

    @Column(name = "wrap_nonce", nullable = false, columnDefinition = "bytea")
    private byte[] wrapNonce;

    @Column(name = "vault_version", nullable = false)
    private int vault_version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kdf_contexts", nullable = false, columnDefinition = "jsonb")
    private KdfContexts kdfContexts;

    @Column(name = "recipient_public_key", nullable = false, columnDefinition = "bytea")
    private byte[] recipientPublicKey;

    @Column(name = "encrypted_recipient_private_key", nullable = false, columnDefinition = "bytea")
    private byte[] encryptedRecipientPrivateKey;

    @Column(name = "recipient_private_key_wrap_nonce", nullable = false, columnDefinition = "bytea")
    private byte[] recipientPrivateKeyWrapNonce;

    @Column(name = "owner_sign_public_key", nullable = false, columnDefinition = "bytea")
    private byte[] ownerSignPublicKey;

    @Column(name = "encrypted_owner_sign_private_key", nullable = false, columnDefinition = "bytea")
    private byte[] encryptedOwnerSignPrivateKey;

    @Column(name = "owner_sign_private_key_wrap_nonce", nullable = false, columnDefinition = "bytea")
    private byte[] ownerSignPrivateKeyWrapNonce;

//    @Column(name = "encrypted_master_key_recovery", nullable = false, columnDefinition = "bytea")
//    private byte[] encryptedMasterKeyRecovery ;
//
//    @Column(name = "recovery_kdf_salt", nullable = false, columnDefinition = "bytea")
//    private byte[] recoveryKdfSalt;
//
//    @Column(name = "recovery_kdf_params", nullable = false, columnDefinition = "jsonb")
//    private String recoveryKdfParams;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected UserKey() {
    }

    public UserKey(User user, byte[] encryptedMasterKey, byte[] kdfSalt, KdfParams kdfParams, byte[] wrapNonce, int vault_version, KdfContexts kdfContexts, byte[] recipientPublicKey, byte[] encryptedRecipientPrivateKey, byte[] recipientPrivateKeyWrapNonce, byte[] ownerSignPublicKey, byte[] encryptedOwnerSignPrivateKey, byte[] ownerSignPrivateKeyWrapNonce) {
        this.user = user;
        this.encryptedMasterKey = encryptedMasterKey;
        this.kdfSalt = kdfSalt;
        this.kdfParams = kdfParams;
        this.wrapNonce = wrapNonce;
        this.vault_version = vault_version;
        this.kdfContexts = kdfContexts;
        this.recipientPublicKey = recipientPublicKey;
        this.encryptedRecipientPrivateKey = encryptedRecipientPrivateKey;
        this.recipientPrivateKeyWrapNonce = recipientPrivateKeyWrapNonce;
        this.ownerSignPublicKey = ownerSignPublicKey;
        this.encryptedOwnerSignPrivateKey = encryptedOwnerSignPrivateKey;
        this.ownerSignPrivateKeyWrapNonce = ownerSignPrivateKeyWrapNonce;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public byte[] getEncryptedMasterKey() {
        return encryptedMasterKey;
    }

    public byte[] getKdfSalt() {
        return kdfSalt;
    }

    public KdfParams getKdfParams() {
        return kdfParams;
    }

    public byte[] getWrapNonce() {
        return wrapNonce;
    }

    public int getVault_version() {
        return vault_version;
    }

    public KdfContexts getKdfContexts() {
        return kdfContexts;
    }

    public byte[] getRecipientPublicKey() {
        return recipientPublicKey;
    }

    public byte[] getEncryptedRecipientPrivateKey() {
        return encryptedRecipientPrivateKey;
    }

    public byte[] getRecipientPrivateKeyWrapNonce() {
        return recipientPrivateKeyWrapNonce;
    }

    public byte[] getOwnerSignPublicKey() {
        return ownerSignPublicKey;
    }

    public byte[] getEncryptedOwnerSignPrivateKey() {
        return encryptedOwnerSignPrivateKey;
    }

    public byte[] getOwnerSignPrivateKeyWrapNonce() {
        return ownerSignPrivateKeyWrapNonce;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

