package lt.pauliusbaksys.datavault.model;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// Allows this class to be used as a composite ID.
@Embeddable
public class PolicyFileId implements Serializable {

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    protected PolicyFileId() {
    }

    public PolicyFileId(UUID policyId, UUID fileId) {
        this.policyId = policyId;
        this.fileId = fileId;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof PolicyFileId that)) return false;
        return Objects.equals(policyId, that.policyId) && Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(policyId, fileId);
    }
}
