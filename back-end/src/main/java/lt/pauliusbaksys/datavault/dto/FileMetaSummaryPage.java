package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record FileMetaSummaryPage(
        List<FileMetaSummary> fileMetaSummary,
        long totalElements
) {}
