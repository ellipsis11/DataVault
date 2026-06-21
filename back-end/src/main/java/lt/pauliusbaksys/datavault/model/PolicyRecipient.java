package lt.pauliusbaksys.datavault.model;
import jakarta.persistence.*;
import lt.pauliusbaksys.datavault.dto.Manifest;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "conditional_release_policy_recipients")
public class PolicyRecipient {

    @EmbeddedId
    private PolicyRecipientId id;

    @MapsId("policyId") // Maps this relation to the policyId part of the composite key.
    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @MapsId("recipientId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Column(name = "encrypted_policy_key_for_recipient", nullable = false)
    private byte[] encryptedPolicyKeyForRecipient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Manifest manifest;

    @Column(name = "signature_by_owner", nullable = false)
    private byte[] signatureByOwner;

    @Column(name = "access_count", nullable = false)
    private int accessCount = 0;

    @Column(name = "first_access_at")
    private OffsetDateTime firstAccessAt;

    @Column(name = "last_access_at")
    private OffsetDateTime lastAccessAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected PolicyRecipient() {
    }

    public PolicyRecipient(PolicyRecipientId id, Policy policy, User recipient, byte[] encryptedPolicyKeyForRecipient, Manifest manifest, byte[] signatureByOwner, OffsetDateTime createdAt) {
        this.id = id;
        this.policy = policy;
        this.recipient = recipient;
        this.encryptedPolicyKeyForRecipient = encryptedPolicyKeyForRecipient;
        this.manifest = manifest;
        this.signatureByOwner = signatureByOwner;
        this.createdAt = createdAt;
    }

    public PolicyRecipientId getId() {
        return id;
    }

    public Policy getPolicy() {
        return policy;
    }

    public User getRecipient() {
        return recipient;
    }

    public byte[] getEncryptedPolicyKeyForRecipient() {
        return encryptedPolicyKeyForRecipient;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public byte[] getSignatureByOwner() {
        return signatureByOwner;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public OffsetDateTime getFirstAccessAt() {
        return firstAccessAt;
    }

    public OffsetDateTime getLastAccessAt() {
        return lastAccessAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }

    public void setFirstAccessAt(OffsetDateTime firstAccessAt) {
        this.firstAccessAt = firstAccessAt;
    }

    public void setLastAccessAt(OffsetDateTime lastAccessAt) {
        this.lastAccessAt = lastAccessAt;
    }
}
