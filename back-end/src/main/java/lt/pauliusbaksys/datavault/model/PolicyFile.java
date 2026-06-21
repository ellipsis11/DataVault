package lt.pauliusbaksys.datavault.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "conditional_release_policy_files")
public class PolicyFile {

    // Stores the composite primary key.
    @EmbeddedId
    private PolicyFileId id;

    @MapsId("policyId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @MapsId("fileId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "encrypted_file_key_by_policy", nullable = false)
    private byte[] encryptedFileKeyByPolicy;

    @Column(name = "encrypted_file_key_nonce", nullable = false)
    private byte[] encryptedFileKeyNonce;

    @Column(name = "encrypted_meta_key_by_policy", nullable = false)
    private byte[] encryptedMetaKeyByPolicy;

    @Column(name = "encrypted_meta_key_nonce", nullable = false)
    private byte[] encryptedMetaKeyNonce;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected PolicyFile() {
    }

    public PolicyFile(PolicyFileId id, Policy policy, File file, byte[] encryptedFileKeyByPolicy, byte[] encryptedFileKeyNonce, byte[] encryptedMetaKeyByPolicy, byte[] encryptedMetaKeyNonce, OffsetDateTime createdAt) {
        this.id = id;
        this.policy = policy;
        this.file = file;
        this.encryptedFileKeyByPolicy = encryptedFileKeyByPolicy;
        this.encryptedFileKeyNonce = encryptedFileKeyNonce;
        this.encryptedMetaKeyByPolicy = encryptedMetaKeyByPolicy;
        this.encryptedMetaKeyNonce = encryptedMetaKeyNonce;
        this.createdAt = createdAt;
    }

    public PolicyFileId getId() {
        return id;
    }

    public Policy getPolicy() {
        return policy;
    }

    public File getFile() {
        return file;
    }

    public byte[] getEncryptedFileKeyByPolicy() {
        return encryptedFileKeyByPolicy;
    }

    public byte[] getEncryptedFileKeyNonce() {
        return encryptedFileKeyNonce;
    }

    public byte[] getEncryptedMetaKeyByPolicy() {
        return encryptedMetaKeyByPolicy;
    }

    public byte[] getEncryptedMetaKeyNonce() {
        return encryptedMetaKeyNonce;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

