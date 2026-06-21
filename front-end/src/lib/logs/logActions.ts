import { apiGet } from '$lib/utils/fetch';
import { API_BASE } from '$lib/config';
import type {
	AuditLogPage,
	AdminAuditLogPage,
	AdminAuditLogListItemDetails,
	AuditActionFilter
} from './logTypes';

export async function getAuditLogPage(params: {
	query: string;
	filter: AuditActionFilter | null;
	page: number;
	size: 10 | 20 | 50 | 100;
}): Promise<AuditLogPage> {
	const paramsToSend = new URLSearchParams();

	paramsToSend.set('query', String(params.query));
	paramsToSend.set('page', String(params.page));
	paramsToSend.set('size', String(params.size));

	if (params.filter) {
		paramsToSend.set('filter', params.filter);
	}

	return apiGet<AuditLogPage>(
		'/audit-logs',
		'Nepavyko gauti naudotojų audito registrų!',
		paramsToSend
	);
}

function createObjectUrl(blob: Blob) {
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = 'audit-logs.csv';
	document.body.appendChild(a);
	a.click();
	a.remove();

	URL.revokeObjectURL(url);
}

export async function exportAuditLogs(): Promise<void> {
	const res = await fetch(`${API_BASE}/audit-logs/export`, {
		credentials: 'include'
	});

	if (!res.ok) throw new Error('Nepavyko eksportuoti audito registrų!');
	const blob = await res.blob();
	createObjectUrl(blob);
}

// --- Actions for admin ---

export async function getAdminAuditLogPage(params: {
	userId?: string;
	query: string;
	filter?: AuditActionFilter | null;
	page: number;
	size: 10 | 20 | 50 | 100;
}): Promise<AdminAuditLogPage> {
	const paramsToSend = new URLSearchParams();

	paramsToSend.set('query', String(params.query));
	paramsToSend.set('page', String(params.page));
	paramsToSend.set('size', String(params.size));

	if (params.userId) {
		paramsToSend.set('userId', params.userId);
	}

	if (params.filter) {
		paramsToSend.set('filter', params.filter);
	}

	return apiGet<AdminAuditLogPage>(
		'/admin/audit-logs',
		'Nepavyko gauti audito registrų puslapio!',
		paramsToSend
	);
}

export async function exportAllAuditLogsForAdmin(): Promise<void> {
	const res = await fetch(`${API_BASE}/admin/audit-logs/export`, {
		credentials: 'include'
	});

	if (!res.ok) throw new Error('Nepavyko eksportuoti audito registrų!');
	const blob = await res.blob();
	createObjectUrl(blob);
}

export async function getAdminAuditLogDetails(
	logId: string
): Promise<AdminAuditLogListItemDetails> {
	return apiGet<AdminAuditLogListItemDetails>(
		`/admin/audit-logs/${logId}`,
		'Nepavyko gauti audito registrų duomenų!'
	);
}

export async function getAuditLogsByProvideDates(
	fromDate: string,
	toDate: string,
	page: number,
	size: number
): Promise<AdminAuditLogPage> {
	return apiGet<AdminAuditLogPage>(
		`/admin/audit-logs`,
		'Nepavyko gauti visų audito registrų puslapio!',
		new URLSearchParams({
			fromDate,
			toDate,
			page: String(page),
			size: String(size)
		})
	);
}
