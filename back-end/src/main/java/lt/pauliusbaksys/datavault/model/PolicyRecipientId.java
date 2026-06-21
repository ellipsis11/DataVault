package lt.pauliusbaksys.datavault.model;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PolicyRecipientId implements Serializable {

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    protected PolicyRecipientId() {
    }

    public PolicyRecipientId(UUID policyId, UUID recipientId) {
        this.policyId = policyId;
        this.recipientId = recipientId;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof PolicyRecipientId that)) return false;
        return Objects.equals(policyId, that.policyId) && Objects.equals(recipientId, that.recipientId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(policyId, recipientId);
    }
}
