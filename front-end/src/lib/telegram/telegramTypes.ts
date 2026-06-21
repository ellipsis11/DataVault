export type TelegramLinkCode = {
	code: string;
	expiresAt: string;
};

export type TelegramLinkStatus = {
	telegramLinked: boolean;
	telegramUsername: string | null;
	connectedAt: string | null;
	chatId: string | null;
};
