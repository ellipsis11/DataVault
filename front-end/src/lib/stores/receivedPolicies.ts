import { writable } from 'svelte/store';
import { API_BASE } from '$lib/config';

export const receivedPoliciesCount = writable(0);
export const newReceivedPoliciesCount = writable(0);

export async function loadReceivedPoliciesCounts(): Promise<void> {
	const res = await fetch(`${API_BASE}/received-policies/counts`, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Nepavyko gauti gautų politikų skaičiaus!');

	const data = await res.json();
	receivedPoliciesCount.set(data.totalCount);
	newReceivedPoliciesCount.set(data.newCount);
}
