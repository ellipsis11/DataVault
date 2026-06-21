import { writable } from 'svelte/store';

type ToastType = 'success' | 'error' | 'info';

export type Toast = {
	open: boolean;
	type: ToastType;
	message: string;
};

export const toast = (() => {
	// Creating a Svelte store with toast state object (open, type, message) and methods to show/hide toasts.
	const { subscribe, set, update } = writable<Toast>({ open: false, type: 'error', message: '' });

	let timer: ReturnType<typeof setTimeout> | null = null;

	function show(type: ToastType, message: string, ms = 3000) {
		if (timer) clearTimeout(timer);
		set({ open: true, type, message });

		if (ms > 0) {
			timer = setTimeout(() => {
				update((t) => ({ ...t, open: false }));
				timer = null;
			}, ms);
		}
	}

	function hide() {
		if (timer) clearTimeout(timer);
		timer = null;
		update((t) => ({ ...t, open: false }));
	}

	return {
		subscribe, // Needed that Svelte can use $toast to acccess the store value reactively in components
		hide,
		success: (message: string, ms?: number) => show('success', message, ms ?? 2500),
		error: (message: string, ms?: number) => show('error', message, ms ?? 4500),
		info: (message: string, ms?: number) => show('info', message, ms ?? 3000)
	};
})();
