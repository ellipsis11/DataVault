package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record PolicyPage(
        List<PolicyListItemDto> policyList,
        long totalElements
) {}
