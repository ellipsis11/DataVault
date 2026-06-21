import { apiPost, type TelegramLinkStatus, type TelegramLinkCode, apiGet } from '$lib';
import { API_BASE } from '$lib';

export async function getTelegramCodeFromApi(): Promise<TelegramLinkCode> {
	return apiPost<TelegramLinkCode>(
		'/telegram/link',
		'Įvyko klaida generuojant Telegram susiejimo kodą!',
		{}
	);
}

export async function fetchTelegramLinkStatusFromApi(): Promise<TelegramLinkStatus> {
	return apiGet<TelegramLinkStatus>(
		'/telegram/link-status',
		'Nepavyko gauti Telegram susiejimo būsenos!'
	);
}

export async function sendTestMessageToTelegram() {
	return apiPost<void>(
		'/telegram/test-message',
		'Nepavyko išsiųsti bandomojo pranešimo į Telegram.',
		{}
	);
}

export async function unlinkTelegram() {
	const res = await fetch(`${API_BASE}/telegram/link`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) {
		const errorText = await res.text();
		throw new Error(errorText || 'Nepavyko atsieti Telegram.');
	}
}
