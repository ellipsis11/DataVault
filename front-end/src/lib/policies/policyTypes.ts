export type PolicyStatus = 'ACTIVE' | 'IN_GRACE' | 'RELEASED' | 'PAUSED';

export type PolicyType = 'INACTIVITY' | 'DATE_TIME' | 'MANUAL_RELEASE';

export type PolicyListItem = {
	policyId: string;
	policyName: string;
	filesCount: number;
	recipientsCount: number;
	policyStatus: PolicyStatus;
	policyType: PolicyType;
};

export type PolicyPage = {
	policyList: PolicyListItem[];
	totalElements: number;
};

export type ReceivedPolicyListItem = {
	policyId: string;
	policyName: string;
	releasedBy: string;
	filesCount: number;
	releasedAt: string;
	viewed: boolean;
};

export type ReceivedPolicyPage = {
	receivedPolicyList: ReceivedPolicyListItem[];
	totalElements: number;
};

export type UserSearchItem = {
	id: string;
	email: string;
};

export type FileSearchItem = {
	id: string;
	fileName?: string;
	createdAt: string;
	loading?: boolean;
};

type UserDialogData = {
	dialogOpen: boolean;
	initLoading: boolean;
	searchResults: UserSearchItem[];
	selectedItems: UserSearchItem[];
};

type FileDialogData = {
	dialogOpen: boolean;
	initLoading: boolean;
	searchResults: FileSearchItem[];
	selectedItems: FileSearchItem[];
};

export type Dialogs = {
	users: UserDialogData;
	files: FileDialogData;
};

export type PolicyReleaseType = 'INACTIVITY' | 'DATE_TIME' | 'MANUAL_RELEASE' | null;

export type WarningChannel = 'TELEGRAM';

type BasePolicy = {
	policyId?: string;
	policyName: string;
	channel: WarningChannel;
	recipients: string[];
	files: string[];
	createdAt?: string;
};

type InactivityPolicy = {
	releaseType: 'INACTIVITY';
	inactivityDays: number | null;
	graceDays: number | null;
	warningEveryHours: number | null;
};

type DateTimePolicy = {
	releaseType: 'DATE_TIME';
	scheduledReleaseAt: string;
	warningBeforeDays: number | null;
};

type ManualReleasePolicy = {
	releaseType: 'MANUAL_RELEASE';
};

type PolicyVariant = InactivityPolicy | DateTimePolicy | ManualReleasePolicy;
export type DialogSelections = BasePolicy & PolicyVariant;

export type PolicyFile = {
	id: string;
	fileName?: string;
	fileSize?: number;
	createdAt: string;
	loading?: boolean;
};

export type PolicyRecipient = {
	id: string;
	email: string;
	totalAccesses: number;
	firstAccessedAt: string;
	lastAccessedAt: string;
};

type PolicyDetailsBase = {
	policyId: string;
	policyName: string;
	channel: WarningChannel;
	policyStatus: PolicyStatus;
	policyType: PolicyType;
	nextAction: string;
	createdAt: string;
	policyFiles: PolicyFile[];
	policyRecipients: PolicyRecipient[];
};

export type PolicyDetails = PolicyDetailsBase & PolicyVariant;

export type ReceivedPolicyDetails = {
	policyId: string;
	policyName: string;
	releasedBy: string;
	releasedAt: string;
	releaseType: PolicyReleaseType;
	viewed: boolean;
	firstAccessAt: string;
	accessCount: number;
	lastAccessAt: string;
	policyFiles: PolicyFile[];
};
export type DecryptPurpose = 'create' | 'view';

export type DecryptableRow = {
	id: string;
	loading?: boolean;
	fileName?: string;
	fileSize?: number;
};

export type PolicyFormMode = 'create' | 'edit';

export type RecipientValidationItem = {
	recipientId: string;
	telegramLinked: boolean;
	keysGenerated: boolean;
};
