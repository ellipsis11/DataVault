import { type AuditLogListItem } from '$lib/logs/logTypes';

export type AdminUserListItem = {
	id: string;
	email: string | null;
	role: 'USER' | 'ADMIN';
	lastLoginAt: string | null;
	createdAt: string;
	logCount: number;
};

export type AdminUserPage = {
	userList: AdminUserListItem[];
	totalElements: number;
};

export type AdminUserDetails = AdminUserListItem & {
	telegramLinked: boolean;
	telegramUsername: string | null;
	telegramConnectedAt: string | null;
	keysGenerated: boolean;
	keysGeneratedAt: string | null;
	ownedPolicyCount: number;
	receivedPolicyCount: number;
	uploadedFileCount: number;
	recentLogs: AuditLogListItem[];
};

export type Color = { bg: string; text: string; border: string };

export type User = {
	userId: string;
	emai: string;
	role: 'USER' | 'ADMIN';
};
