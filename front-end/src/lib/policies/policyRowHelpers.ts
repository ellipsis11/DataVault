import { type PolicyStatus, type PolicyType } from './policyTypes';

export function translatePolicyType(type: PolicyType) {
	switch (type) {
		case 'INACTIVITY':
			return 'Neaktyvumo';
		case 'DATE_TIME':
			return 'Nustatyto laiko';
		case 'MANUAL_RELEASE':
			return 'Rankinio atskleidimo';
	}
}

export function translatePolicyStatus(status: PolicyStatus) {
	switch (status) {
		case 'ACTIVE':
			return 'Aktyvi';
		case 'IN_GRACE':
			return 'Atidėjimo periode';
		case 'RELEASED':
			return 'Atskleista';
		case 'PAUSED':
			return 'Pauzėje';
	}
}
