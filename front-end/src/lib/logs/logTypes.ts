export type AuditLevel = 'INFO' | 'ALERT';

export type AuditAction =
	| 'REGISTER'
	| 'LOGIN'
	| 'LOGOUT'
	| 'USER_KEYS_CREATE'
	| 'FILE_UPLOAD'
	| 'FILE_RENAME'
	| 'FILE_DELETE'
	| 'FILE_DOWNLOAD'
	| 'POLICY_CREATE'
	| 'POLICY_UPDATE'
	| 'POLICY_DELETE'
	| 'POLICY_PAUSE'
	| 'POLICY_RESUME'
	| 'POLICY_WARNING_SEND'
	| 'POLICY_GRACE_STARTED'
	| 'POLICY_RELEASE'
	| 'RECEIVED_POLICY_ACCESS'
	| 'RECEIVED_FILE_DOWNLOAD'
	| 'TELEGRAM_LINK'
	| 'TELEGRAM_UNLINK'
	| 'USER_DELETE';

export type AuditLogListItem = {
	id: string;
	level: AuditLevel;
	actionType: AuditAction;
	message: string;
	createdAt: string;
	hashValid: boolean;
};

export type AuditLogPage = {
	logList: AuditLogListItem[];
	totalElements: number;
};

export type LogTablePurpose = 'user' | 'admin';

export type AdminAuditLogListItem = {
	id: string;
	userId: string | null;
	actorEmail: string | null;
	level: AuditLevel;
	actionType: AuditAction;
	message: string;
	createdAt: string;
	hashValid: boolean;
};

export type AdminAuditLogPage = {
	logList: AdminAuditLogListItem[];
	totalElements: number;
	logChainIntegrityValid: boolean;
};

export type AdminAuditLogListItemDetails = AdminAuditLogListItem & {
	metaData: Record<string, object> | null;
};

export type AuditActionFilter = 'ACCOUNT' | 'FILES' | 'POLICIES' | 'TELEGRAM';
