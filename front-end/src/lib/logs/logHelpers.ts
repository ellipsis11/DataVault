import { type AuditAction, type AuditLevel } from '$lib';

export function translateLogLevel(level: AuditLevel) {
	switch (level) {
		case 'INFO':
			return 'Informacija';
		case 'ALERT':
			return 'Įspėjimas';
	}
}

export function translateLogAction(action: AuditAction) {
	switch (action) {
		case 'REGISTER':
			return 'Registracija';
		case 'LOGIN':
			return 'Prisijungimas';
		case 'LOGOUT':
			return 'Atsijungimas';
		case 'USER_KEYS_CREATE':
			return 'Naudotojo raktų kūrimas';
		case 'FILE_UPLOAD':
			return 'Failo įkėlimas';
		case 'FILE_RENAME':
			return 'Failo pervadinimas';
		case 'FILE_DELETE':
			return 'Failo ištrynimas';
		case 'FILE_DOWNLOAD':
			return 'Failo atsisiuntimas';
		case 'POLICY_CREATE':
			return 'Politikos kūrimas';
		case 'POLICY_UPDATE':
			return 'Politikos atnaujinimas';
		case 'POLICY_DELETE':
			return 'Politikos ištrynimas';
		case 'POLICY_PAUSE':
			return 'Politikos pauzė';
		case 'POLICY_RESUME':
			return 'Politikos pratęsimas';
		case 'POLICY_WARNING_SEND':
			return 'Politikos įspėjimo siuntimas';
		case 'POLICY_GRACE_STARTED':
			return 'Politikos leidimo pradžia';
		case 'POLICY_RELEASE':
			return 'Politikos išleidimas';
		case 'RECEIVED_POLICY_ACCESS':
			return 'Gautas politikos prieiga';
		case 'RECEIVED_FILE_DOWNLOAD':
			return 'Gautas failo atsisiuntimas';
		case 'TELEGRAM_LINK':
			return 'Telegram nuoroda';
		case 'TELEGRAM_UNLINK':
			return 'Telegram atsijungimas';
		case 'USER_DELETE':
			return 'Naudotojas pašalintas';
	}
}
