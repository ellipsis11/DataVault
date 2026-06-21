package lt.pauliusbaksys.datavault.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NewPolicy(
        PolicyDto policy,
        @NotEmpty List<EncryptedPolicyFileKey> encryptedFileKeys,
        @NotEmpty List<RecipientPolicyAccess> recipientAccesses
) {}