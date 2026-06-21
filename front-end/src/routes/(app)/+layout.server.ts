import { redirect } from '@sveltejs/kit';
import { API_BASE } from '$lib';

export async function load({ fetch, setHeaders, cookies }) {
	cookies.delete('JSESSIONID', { path: '/' });
	setHeaders({ 'cache-control': 'no-store' }); // Fixes the go back. Not caching this page response.

	const res = await fetch(`${API_BASE}/auth/me`, {
		credentials: 'include'
	});

	if (!res.ok) {
		throw redirect(303, '/auth');
	}

	const user = await res.json();

	return { user };
}
