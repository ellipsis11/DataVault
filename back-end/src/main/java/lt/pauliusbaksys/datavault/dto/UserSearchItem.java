package lt.pauliusbaksys.datavault.dto;
import java.util.UUID;

public record UserSearchItem(
        UUID id,
        String email
) {}