package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record AdminUserPage(
        List<AdminUserListItem> userList,
        long totalElements
) {}
