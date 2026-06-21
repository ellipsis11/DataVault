package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record FileMetaFullPage(
        List<FileMetaFull> fileMetaFull,
        long totalElements
) {}
