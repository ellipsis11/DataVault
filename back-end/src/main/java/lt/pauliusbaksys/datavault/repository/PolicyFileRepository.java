package lt.pauliusbaksys.datavault.repository;

import lt.pauliusbaksys.datavault.model.PolicyFile;
import lt.pauliusbaksys.datavault.model.PolicyFileId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface PolicyFileRepository extends JpaRepository<PolicyFile, PolicyFileId> {
    List<PolicyFile> findAllByPolicy_Id(UUID policyId);
    void deleteAllByPolicy_Id(UUID policyId);
    Optional<PolicyFile> findByPolicy_IdAndFile_Id(UUID policyId, UUID fileId);
    int countByPolicy_Id(UUID policyId);
}
