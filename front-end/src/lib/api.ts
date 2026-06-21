import { API_BASE } from './config';
import type { PolicyStatus } from './policies/policyTypes';
import { apiGet } from './utils/fetch';

type Role = 'ADMIN' | 'USER';

export type AuthUser = {
	userId: string;
	email: string;
	role: Role;
};

export function convertFileSize(bytes: number): string {
	if (bytes < 1000) return `${bytes} B`;
	if (bytes < 1000_000) return `${(bytes / 1024).toFixed(2)} KB`;
	if (bytes < 1000_000_000) return `${(bytes / 1048_576).toFixed(2)} MB`;
	return `${(bytes / 1073_741_824).toFixed(2)} GB`;
}

export const shortFileName = (name?: string) =>
	!name ? '' : name.length <= 20 ? name : `${name.slice(0, 8)}...${name.slice(-8)}`;

export async function getVaultStatus(): Promise<'initialized' | 'not_initialized'> {
	const res = await fetch(`${API_BASE}/vault/status`, { credentials: 'include' });
	if (res.status === 404) return 'not_initialized';
	if (res.ok) return 'initialized';
	throw new Error(`Nepavyko patikrinti saugyklos būsenos: ${res.status}`);
}

export async function getMe(): Promise<AuthUser> {
	const res = await fetch(`${API_BASE}/auth/me`, { credentials: 'include' });
	if (!res.ok) throw new Error(`Nepavyko gauti dabartinio naudotojo duomenų: ${res.status}`);
	return res.json();
}

export async function searchPage<T, S extends string = string>(
	url: string,
	errorMessage: string,
	params: {
		query: string;
		filter?: S | null;
		page: number;
		size: number;
	}
): Promise<T> {
	const searchParams = new URLSearchParams({
		query: params.query,
		page: String(params.page),
		size: String(params.size)
	});

	if (params.filter) {
		searchParams.set('filter', params.filter);
	}

	return apiGet<T>(url, errorMessage, searchParams);
}

/**
 * For escaping the special regex characters like "." and "?"
 * so those are searched as normal text, not as regex syntax
 */
function escapeRegex(value: string) {
	return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Highlighting all matches of the search query inside the given text.
 *
 * Matching is global (g) and case-insensitive (i) meaning
 * all matches are highlighted regardless of letter case.
 *
 * The matched text is wrapped with the HTML <mark> element.
 *
 * Example:
 * highlightText("File uploaded: secret-file.txt", "file")
 * returns "<mark>File</mark> uploaded: secret-<mark>file</mark>.txt"
 */
export function highlightText(text: string, query: string) {
	const q = query.trim();
	if (!q) return text;
	const regex = new RegExp(`(${escapeRegex(q)})`, 'gi');

	return text.replace(regex, '<mark>$1</mark>');
}

export async function getErrorMessage(res: Response, fallback: string): Promise<string> {
	try {
		const errorBody = await res.json();
		return errorBody.message || fallback;
	} catch {
		return fallback;
	}
}
