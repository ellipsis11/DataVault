import { redirect } from '@sveltejs/kit';
import { API_BASE } from '$lib';

export const actions = {
	default: async ({ fetch, cookies }) => {
		try {
			await fetch(`${API_BASE}/api/auth/logout`, {
				method: 'POST',
				credentials: 'include'
			});

			cookies.delete('access_token', { path: '/' });
			cookies.delete('JSESSIONID', { path: '/' });
		} finally {
			throw redirect(303, '/auth');
		}
	}
};
