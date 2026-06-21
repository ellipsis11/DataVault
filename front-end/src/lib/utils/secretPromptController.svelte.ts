export function createPromptController() {
	let open = $state<boolean>(false);
	let loading = $state<boolean>(false);

	// Null because we dont have these functions yet
	let resolveFn: ((v: string) => void) | null = null;
	let rejectFn: ((reason?: any) => void) | null = null;

	// Creating a new Promise<string>
	function ask(): Promise<string> {
		open = true;
		return new Promise<string>((resolve, reject) => {
			resolveFn = resolve;
			rejectFn = reject;
		});
	}

	function submit(v: string) {
		loading = true;
		resolveFn?.(v);
		resolveFn = null;
		rejectFn = null;
	}

	function close(errorText?: string) {
		open = false;
		loading = false;
		rejectFn?.(new Error(errorText ?? 'Įvyko klaida!'));
		rejectFn = null;
		resolveFn = null;
	}

	return {
		get open() {
			return open;
		},
		get loading() {
			return loading;
		},
		ask,
		submit,
		close
	};
}
