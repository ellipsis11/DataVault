package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record ReceivedPolicyPage(
        List<ReceivedPolicyListItemDto> receivedPolicyList,
        long totalElements
) {}
