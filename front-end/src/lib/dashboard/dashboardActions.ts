import { apiGet } from '$lib/utils/fetch';
import type { AdminDashboardData, UserDashboardData } from './dashboardTypes';

export async function getDataForUserDashboard() {
	return apiGet<UserDashboardData>(
		'/dashboard/user',
		'Nepavyko gauti duomenų vartotojo informacijos suvestinei'
	);
}

export async function getDataForAdminDashboard() {
	return apiGet<AdminDashboardData>(
		'/dashboard/admin',
		'Nepavyko gauti duomenų administratoriaus suvestinei'
	);
}
