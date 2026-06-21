import { type AdminAuditLogListItem, type AuditLogListItem } from '$lib';

export type UserDashboardData = {
	totalFiles: number;
	totalPolicies: number;
	totalReleasedPolicies: number;
	totalNewPolicies: number;
	telegramLinked: boolean;
	encryptionKeysGenerated: boolean;
	auditLogs: AuditLogListItem[];
};

export type AdminDashboardData = {
	totalUsers: number;
	totalFiles: number;
	totalPolicies: number;
	totalAuditLogs: number;
	auditLogs: AdminAuditLogListItem[];
	newUsersToday: number;
	newFilesToday: number;
	newPoliciesToday: number;
};
