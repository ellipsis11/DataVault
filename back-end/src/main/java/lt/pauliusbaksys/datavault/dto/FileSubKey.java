package lt.pauliusbaksys.datavault.dto;
import java.util.UUID;

public record FileSubKey(
        UUID fileId,
        String subKeyId
) {}
