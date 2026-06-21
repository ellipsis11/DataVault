import { API_BASE } from '$lib/config';
import { apiGet } from '$lib/utils/fetch';
import { type AdminUserPage, type AdminUserDetails } from './userTypes';

export async function getAdminUsersPage(params: {
	query: string;
	page: number;
	size: 10 | 20 | 50 | 100;
}): Promise<AdminUserPage> {
	return apiGet<AdminUserPage>(
		'/admin/users',
		'Nepavyko gauti naudotojų puslapio!',
		new URLSearchParams({
			query: String(params.query),
			page: String(params.page),
			size: String(params.size)
		})
	);
}

export async function getAdminUserDetails(userId: string): Promise<AdminUserDetails> {
	return apiGet<AdminUserDetails>(
		`/admin/users/${userId}`,
		'Nepavyko gauti naudotojų informacijos!'
	);
}

export async function deleteUser(userId: string): Promise<void> {
	const res = await fetch(`${API_BASE}/admin/users/${userId}`, {
		method: 'DELETE',
		credentials: 'include'
	});

	if (!res.ok) {
		const message = await res.text();
		throw new Error(message || 'Naudotojo ištrinti nepavyko!');
	}
}
