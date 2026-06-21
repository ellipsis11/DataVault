package lt.pauliusbaksys.datavault.enums;

import java.util.EnumSet;

public enum AuditLogFilter {
    ACCOUNT(EnumSet.of(
            AuditAction.REGISTER,
            AuditAction.LOGIN,
            AuditAction.LOGOUT,
            AuditAction.USER_KEYS_CREATE
    )),

    FILES(EnumSet.of(
            AuditAction.FILE_UPLOAD,
            AuditAction.FILE_RENAME,
            AuditAction.FILE_DELETE,
            AuditAction.FILE_DOWNLOAD,
            AuditAction.RECEIVED_FILE_DOWNLOAD
    )),

    POLICIES(EnumSet.of(
            AuditAction.POLICY_CREATE,
            AuditAction.POLICY_UPDATE,
            AuditAction.POLICY_DELETE,
            AuditAction.POLICY_PAUSE,
            AuditAction.POLICY_RESUME,
            AuditAction.POLICY_WARNING_SEND,
            AuditAction.POLICY_GRACE_STARTED,
            AuditAction.POLICY_RELEASE,
            AuditAction.RECEIVED_POLICY_ACCESS
    )),

    TELEGRAM(EnumSet.of(
            AuditAction.TELEGRAM_LINK,
            AuditAction.TELEGRAM_UNLINK
    ));

    private final EnumSet<AuditAction> actions;

    // Assigning a list of AuditActions to each filter value, and the actions field stores that list.
    AuditLogFilter(EnumSet<AuditAction> actions) {
        this.actions = actions;
    }

    public EnumSet<AuditAction> getActions(){
        return actions;
    }
}