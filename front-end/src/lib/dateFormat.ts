export function formatDate(date: string | number | Date, purpose?: 'date' | 'date&time') {
	try {
		const d = new Date(date);
		let returnValue;

		if (!purpose || purpose !== 'date') {
			returnValue = new Intl.DateTimeFormat('lt-LT', {
				year: 'numeric',
				month: '2-digit',
				day: '2-digit',
				hour: '2-digit',
				minute: '2-digit',
				second: '2-digit'
			}).format(d);
		} else {
			returnValue = new Intl.DateTimeFormat('lt-LT', {
				year: 'numeric',
				month: '2-digit',
				day: '2-digit'
			}).format(d);
		}

		return returnValue;
	} catch (e) {
		return date;
	}
}

export function formatDateParts(date: string | null) {
	if (!date) {
		return {
			date: '-',
			time: ''
		};
	}

	const d = new Date(date);

	return {
		date: d.toLocaleDateString('lt-LT', {
			year: 'numeric',
			month: 'short',
			day: 'numeric'
		}),
		time: d.toLocaleTimeString('lt-LT', {
			hour: '2-digit',
			minute: '2-digit'
		})
	};
}
