import { getErrorMessage } from '$lib/api';
import { API_BASE } from '$lib/config';

export async function apiGet<T>(
	path: string,
	errorMsg: string,
	params?: URLSearchParams
): Promise<T> {
	const url = `${API_BASE}${path}${params ? '?' + params.toString() : ''}`;

	const res = await fetch(url, {
		credentials: 'include'
	});

	if (!res.ok) throw new Error(await getErrorMessage(res, errorMsg));

	// res.json() crashes on empty body
	const text = await res.text();
	if (!text) return undefined as T;
	return JSON.parse(text) as T;
}

export async function apiPost<T>(path: string, errorMsg: string, body: any): Promise<T> {
	const url = `${API_BASE}${path}`;

	const res = await fetch(url, {
		method: 'POST',
		credentials: 'include',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(body)
	});

	if (!res.ok) throw new Error(await getErrorMessage(res, errorMsg));

	const text = await res.text();
	if (!text) return undefined as T;
	return JSON.parse(text) as T;
}
