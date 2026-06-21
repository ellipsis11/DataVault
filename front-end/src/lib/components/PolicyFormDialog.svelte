<script lang="ts">
	import {
		formatDate,
		toast,
		shortFileName,
		type Dialogs,
		type DialogSelections,
		type FileSearchItem,
		type PolicyReleaseType,
		type UserSearchItem,
		type WarningChannel,
		type DecryptPurpose,
		type PolicyFormMode,
		type PolicyDetails,
		type PolicyRecipient,
		type PolicyFile,
		type RecipientValidationItem
	} from '$lib';
	import {
		CloudUpload,
		X,
		Search,
		User,
		File,
		Eye,
		EyeOff,
		TriangleAlert,
		CircleAlert
	} from 'lucide-svelte';

	let {
		mode,
		dialogs,
		payload,
		policyDetails,
		getRecipientValidation,
		telegramMissingCount,
		onResetDialogs,
		keysMissingCount,
		submitButtonText,
		onSearchUsers,
		onFilterFiles,
		onDecryptOne,
		onHideOne,
		onAdd,
		onRemove,
		onSubmit,
		onClose
	} = $props<{
		mode: PolicyFormMode;
		dialogs: Dialogs;
		payload: DialogSelections | null;
		policyDetails: PolicyDetails | null;
		validationResults: RecipientValidationItem[];
		submitButtonText: string | null;
		getRecipientValidation: (recipientId: string) => void;
		telegramMissingCount: () => void;
		keysMissingCount: () => void;
		onResetDialogs: () => void;
		onSearchUsers: (query: string) => void;
		onFilterFiles: (args: { fileFrom?: string; fileTo?: string }) => void;
		onDecryptOne: (fileId: string, purpose: DecryptPurpose) => void;
		onHideOne: (fileId: string, purpose: DecryptPurpose) => void;
		onAdd: (purpose: 'user' | 'file', item: UserSearchItem | FileSearchItem) => boolean;
		onRemove: (purpose: 'user' | 'file', userId: string) => void; // maybe userid to fileid
		onSubmit: (dialogSelections: DialogSelections) => void;
		onClose: () => void;
	}>();
	let usersQuery = $state<string>('');
	let fileFrom = $state<string>('');
	let fileTo = $state<string>('');
	let grace = $state<boolean>(false);
	let warningBeforeDays = $state<boolean>(false);
	let recipients = $derived(dialogs.users.selectedItems.map((u: UserSearchItem) => u.id));
	let files = $derived(dialogs.files.selectedItems.map((f: FileSearchItem) => f.id));
	let triedSubmit: boolean = false;
	type DialogErrors = {
		policyName?: string;
		releaseType?: string;
		inactivity?: string;
		warningBeforeDays?: string;
		grace?: string;
		warningEveryHours?: string;
		scheduledReleaseAt?: string;
		recipients?: string;
		files?: string;
	};
	let errors = $state<DialogErrors>();
	let hideKeysWarning = $state(false);
	let hideTelegramWarning = $state(false);
	let form = $state({
		policyName: '',
		channel: 'TELEGRAM' as WarningChannel,
		recipients: [] as string[],
		files: [] as string[],
		releaseType: null as PolicyReleaseType,
		inactivityDays: 0,
		graceDays: 0,
		warningEveryHours: 0,
		scheduledReleaseAt: '',
		warningBeforeDays: 0
	});

	$effect(() => {
		if (mode === 'edit' && policyDetails) {
			form.policyName = policyDetails.policyName;
			form.channel = policyDetails.channel;
			form.recipients = policyDetails.policyRecipients.map((r: PolicyRecipient) => r.id);
			form.files = policyDetails.policyFiles.map((f: PolicyFile) => f.id);
			form.releaseType = policyDetails.releaseType;

			if (policyDetails.releaseType === 'INACTIVITY') {
				form.inactivityDays = policyDetails.inactivityDays ?? 0;
				grace = !!policyDetails.graceDays;
				form.graceDays = policyDetails.graceDays ?? 0;
				form.warningEveryHours = policyDetails.warningEveryHours ?? 0;
			} else if (policyDetails.releaseType === 'DATE_TIME') {
				form.scheduledReleaseAt = policyDetails.scheduledReleaseAt
					? policyDetails.scheduledReleaseAt.slice(0, 16)
					: '';
				warningBeforeDays = !!policyDetails.warningBeforeDays;
				form.warningBeforeDays = policyDetails.warningBeforeDays ?? 0;
			}
		}
	});

	$effect(() => {
		form.recipients = recipients;
		form.files = files;

		if (!grace) {
			form.graceDays = 0;
			form.warningEveryHours = 0;
		}

		if (!warningBeforeDays) {
			form.warningBeforeDays = 0;
		}
	});

	function validate(): boolean {
		errors = {};
		const newErrors: DialogErrors = {};

		if (!form.policyName.trim()) {
			newErrors.policyName = 'Politikos pavadinimas yra privalomas!';
		}

		if (!form.releaseType) {
			newErrors.releaseType = 'Pasirinkite sąlyginio atskleidimo politikos tipą!';
		} else {
			if (form.releaseType === 'INACTIVITY') {
				if (form.inactivityDays <= 0) {
					newErrors.inactivity = 'Neaktyvumo dienų skaičius turi būti didesnis nei 0!';
				}

				if (grace && form.graceDays <= 0) {
					newErrors.grace = 'Atidėjimo laikotarpis turi būti didesnis nei 0!';
				}

				if (grace && form.warningEveryHours <= 0) {
					newErrors.warningEveryHours = 'Įspėjimų intervalas valandomis turi būti didesnis nei 0!';
				}
			} else if (form.releaseType === 'DATE_TIME') {
				if (!form.scheduledReleaseAt) {
					newErrors.scheduledReleaseAt = 'Atskleidimo data ir laikas yra privalomi!';
				}

				if (warningBeforeDays && form.warningBeforeDays <= 0) {
					newErrors.warningBeforeDays = 'Įspėjimo dienų skaičius turi būti didesnis nei 0!';
				}
			}
		}

		if (dialogs.users.selectedItems.length === 0) {
			newErrors.recipients = 'Pasirinkite bent vieną gavėją!';
		}

		if (dialogs.files.selectedItems.length === 0) {
			newErrors.files = 'Pasirinkite bent vieną failą!';
		}

		errors = newErrors;
		return Object.keys(newErrors).length === 0;
	}

	function submit(): void {
		triedSubmit = true;
		if (!validate()) {
			toast.info('Užpildykite visus privalomus laukus teisingai.');
			return;
		}

		onResetDialogs();
		usersQuery = '';
		fileFrom = '';
		fileTo = '';
		const s = $state.snapshot(form);

		switch (form.releaseType) {
			case 'INACTIVITY':
				payload = {
					policyName: s.policyName,
					channel: s.channel,
					recipients: s.recipients,
					files: s.files,
					releaseType: 'INACTIVITY',
					inactivityDays: s.inactivityDays,
					graceDays: s.graceDays > 0 ? s.graceDays : null,
					warningEveryHours: s.graceDays > 0 && s.warningEveryHours > 0 ? s.warningEveryHours : null
				};
				break;

			case 'DATE_TIME':
				payload = {
					policyName: s.policyName,
					channel: s.channel,
					recipients: s.recipients,
					files: s.files,
					releaseType: 'DATE_TIME',
					scheduledReleaseAt: s.scheduledReleaseAt
						? new Date(s.scheduledReleaseAt).toISOString()
						: null,
					warningBeforeDays: s.warningBeforeDays > 0 ? s.warningBeforeDays : null
				};
				break;

			case 'MANUAL_RELEASE':
				payload = {
					policyName: s.policyName,
					channel: s.channel,
					recipients: s.recipients,
					files: s.files,
					releaseType: 'MANUAL_RELEASE'
				};
				break;
		}

		onSubmit(payload);
		submitButtonText = null;
	}

	function maybeValidate(): void {
		if (triedSubmit) {
			validate();
		}
	}

	function close() {
		onClose();
		usersQuery = '';
		fileFrom = '';
		fileTo = '';
	}
</script>

<div class="overlay">
	<div class="modal">
		<div class="modal-header">
			<div class="header-left">
				<span class="cloud"><CloudUpload size={34} /></span>
				<div class="header-text">
					{#if mode === 'create'}
						<h1>Sukurti naują sąlyginio atskleidimo politiką</h1>
						<p>Pasirinkite sąlygas, gavėjus ir failus</p>
					{:else}
						<h1>Redaguoti sąlyginio atskleidimo politiką</h1>
						<p>Pakeiskite sąlygas, gavėjus ir failus</p>
					{/if}
				</div>
			</div>
			<div class="header-right">
				<button type="button" onclick={close}><X /></button>
			</div>
		</div>
		<div class="modal-body">
			<div class="card">
				<div class="field">
					<h2>Politikos pavadinimas</h2>
					<input
						type="text"
						name="policy-name"
						id="policy-name"
						placeholder="Įveskite politikos pavadinimą"
						bind:value={form.policyName}
						oninput={() => maybeValidate()}
					/>
					<p class="error-text">
						{errors?.policyName}
					</p>
				</div>
			</div>
			<div class="card">
				<div class="field">
					<h2>Atskleidimo režimas</h2>
					<select
						name="release_types"
						id="release_types"
						bind:value={form.releaseType}
						onchange={() => maybeValidate()}
					>
						<option value={null} disabled selected>Pasirinkite atskleidimo režimą</option>
						<option value="INACTIVITY">Neaktyvumas</option>
						<option value="DATE_TIME">Suplanuotas atskleidimas</option>
						<option value="MANUAL_RELEASE">Rankinis atskleidimas</option>
					</select>
					<p class="error-text">
						{errors?.releaseType}
					</p>
				</div>
			</div>
			{#if form.releaseType && form.releaseType !== 'MANUAL_RELEASE'}
				<div class="card">
					<h2>Atskleidimo sąlygos</h2>
					<div class="conditions">
						{#if form.releaseType === 'INACTIVITY'}
							<div class="segmented">
								<div class="field">
									<label for="heart"
										>Neaktyvumo laikotarpis <input
											type="number"
											id="heart"
											min="1"
											max="31"
											bind:value={form.inactivityDays}
											oninput={() => maybeValidate()}
										/> dienos</label
									>
								</div>
								<p class="error-text">
									{errors?.inactivity}
								</p>
							</div>
							<div class="segmented">
								<div class="check-block">
									<input
										type="checkbox"
										id="grace"
										bind:checked={grace}
										onchange={() => maybeValidate()}
									/>
									<label for="grace">Atidėjimo laikotarpis</label>
								</div>
								<div class="field">
									<label for="grace-d"
										>Atidėjimo laikotarpis: <input
											type="number"
											id="grace-d"
											min="1"
											max="15"
											bind:value={form.graceDays}
											disabled={!grace}
											oninput={() => maybeValidate()}
										/> dienos</label
									>
								</div>
								<p class="error-text">
									{errors?.grace}
								</p>
								<div class="field">
									<label for="grace-w-e-h"
										>Įspėjimų intervalas valandomis: <input
											type="number"
											id="grace-w-e-h"
											min="1"
											max="15"
											bind:value={form.warningEveryHours}
											disabled={!grace}
											oninput={() => maybeValidate()}
										/> val.</label
									>
								</div>
								<p class="error-text">
									{errors?.warningEveryHours}
								</p>
							</div>
						{:else}
							<div class="segmented">
								<div class="field">
									<label for="release-datetime"
										>Atskleisti failus nurodytą datą ir laiką: <input
											type="datetime-local"
											id="release-datetime"
											bind:value={form.scheduledReleaseAt}
											onchange={() => maybeValidate()}
										/></label
									>
								</div>
								<p class="error-text">
									{errors?.scheduledReleaseAt}
								</p>
							</div>
							<div class="segmented">
								<div class="check-block">
									<input
										type="checkbox"
										id="release-datetime-w-b-d-c"
										bind:checked={warningBeforeDays}
										onchange={() => maybeValidate()}
									/>
									<label for="release-datetime-w-b-d-c">Įspėti likus n dienų iki atskleidimo</label>
								</div>
								<div class="field">
									<label for="release-datetime-w-b-d"
										>Įspėti likus dienų: <input
											type="number"
											id="release-datetime-w-b-d"
											min="1"
											max="31"
											bind:value={form.warningBeforeDays}
											oninput={() => maybeValidate()}
											disabled={!warningBeforeDays}
										/> dienos</label
									>
								</div>
								<p class="error-text">
									{errors?.warningBeforeDays}
								</p>
							</div>
						{/if}
					</div>
				</div>
			{/if}
			<div class="card recipients">
				<h2>Gavėjai</h2>
				<div class="field search">
					<label for="rcp-search" aria-label="Search"><Search size={20} /></label>
					<input
						type="search"
						name="rcp-search"
						id="rcp-search"
						placeholder="Search by email addreas"
						bind:value={usersQuery}
						oninput={() => onSearchUsers(usersQuery)}
					/>
					{#if dialogs.users.dialogOpen}
						<div class="dropdown">
							{#if dialogs.users.initLoading}
								<span class="skeleton-line"></span>
							{:else if dialogs.users.searchResults.length === 0 && usersQuery?.trim() !== ''}
								<span class="italic">Naudotojų nerasta.</span>
							{:else}
								{#each dialogs.users.searchResults as user (user.id)}
									<button
										type="button"
										class="dropdown-item"
										onclick={() => {
											const res = onAdd('user', user);
											!res ? toast.info('Šis naudotojas jau pridėtas.') : (usersQuery = '');
											maybeValidate();
										}}
										><User size={15} />
										<span>{user.email}</span>
									</button>
								{/each}
							{/if}
						</div>
					{/if}
				</div>
				{#if dialogs.users.selectedItems.length > 0}
					<div class="selected-rows" class:single={dialogs.users.selectedItems.length === 1}>
						{#each dialogs.users.selectedItems as r (r.id)}
							{@const validation = getRecipientValidation(r.id)}
							<div class="row">
								<span>{r.email}</span>
								{#if validation && !validation.keysGenerated && !validation.telegramLinked}
									<small class="critical-error">Neparuošta</small>
								{:else if validation && !validation.keysGenerated}
									<small class="critical-error">Raktai nesukurti</small>
								{:else if validation && !validation.telegramLinked}
									<small class="warning">Telegram neprijungtas</small>
								{/if}
								<button type="button" onclick={() => onRemove('user', r.id)}><X size={15} /></button
								>
							</div>
						{/each}
					</div>
				{/if}
				{#if errors?.recipients}
					<p class="error-text">
						{errors?.recipients}
					</p>
				{/if}
				{#if telegramMissingCount() > 0 && !hideTelegramWarning}
					{@const count = telegramMissingCount()}
					<div class="error-bar telegram">
						<i><TriangleAlert size={22} /></i>
						<p>
							{count === 1
								? '1 gavėjas nėra prijungęs „Telegram“. Jis gali negauti atskleidimo pranešimų.'
								: `${count} gavėjai nėra prijungę „Telegram“. Jie gali negauti atskleidimo pranešimų.`}
						</p>
						<button type="button" class="error-close" onclick={() => (hideTelegramWarning = true)}>
							<X size={15} />
						</button>
					</div>
				{/if}
				{#if keysMissingCount() > 0 && !hideKeysWarning}
					{@const count = keysMissingCount()}
					<div class="error-bar keys">
						<i><CircleAlert size={22} /></i>
						<p>
							{count === 1
								? '1 gavėjas nėra sugeneravęs raktų. Politika negali būti sukurta.'
								: `${count} gavėjai nėra sugeneravę raktų. Politika negali būti sukurta.`}
						</p>
						<button type="button" class="error-close" onclick={() => (hideKeysWarning = true)}>
							<X size={15} />
						</button>
					</div>
				{/if}
			</div>
			<div class="card files">
				<h2>Failai</h2>
				<div class="files-filter">
					<div class="field">
						<label for="file-from">Nuo</label>
						<input
							type="date"
							id="file-from"
							name="file-from"
							bind:value={fileFrom}
							onchange={() => onFilterFiles({ fileFrom, fileTo })}
						/>
					</div>
					<div class="dash-wrap" aria-hidden="true">
						<div class="dash"></div>
					</div>
					<div class="field">
						<label for="file-to">Iki</label>
						<input
							type="date"
							id="file-to"
							name="file-to"
							bind:value={fileTo}
							onchange={() => onFilterFiles({ fileFrom, fileTo })}
						/>
					</div>
					{#if dialogs.files.dialogOpen}
						<div class="dropdown">
							{#if dialogs.files.initLoading}
								<span class="skeleton-line"></span>
							{:else if dialogs.files.searchResults.length === 0 && (fileTo?.trim() !== '' || fileFrom?.trim() !== '')}
								<span class="italic">Failų nerasta.</span>
							{:else}
								{#each dialogs.files.searchResults as file (file.id)}
									{#if file.loading}
										<div class="dropdown-item">
											<span class="skeleton-line"></span>
										</div>
									{:else}
										<div
											class="dropdown-item"
											role="button"
											tabindex="0"
											onclick={() => {
												const res = onAdd('file', file);
												!res ? toast.info('Šis failas jau pridėtas.') : (fileFrom = fileTo = '');
												maybeValidate();
											}}
											onkeydown={null}
										>
											<i class="file-icon"><File size={15} /></i>
											<span class="file-name">{shortFileName(file.fileName ?? '')}</span>
											<span class="created-date-time" class:active={!!file.fileName}
												>{formatDate(file.createdAt)}</span
											>
											{#if file.fileName}
												<button
													type="button"
													class="icon-btn"
													onclick={(e) => {
														e.stopPropagation();
														onHideOne(file.id, 'create');
													}}
													aria-label="Preview"
												>
													<Eye size={15} />
												</button>
											{:else}
												<button
													type="button"
													class="icon-btn"
													onclick={(e) => {
														e.stopPropagation();
														onDecryptOne(file.id, 'create');
													}}
													aria-label="Hide"
												>
													<EyeOff size={15} />
												</button>
											{/if}
										</div>
									{/if}
								{/each}
							{/if}
						</div>
					{/if}
				</div>
				{#if dialogs.files.selectedItems.length > 0}
					<div class="selected-rows" class:single={dialogs.files.selectedItems.length === 1}>
						{#each dialogs.files.selectedItems as f (f.id)}
							<div class="row">
								<span class="created-date-time">{formatDate(f.createdAt)}</span>
								<button type="button" onclick={() => onRemove('file', f.id)}><X size={15} /></button
								>
							</div>
						{/each}
					</div>
				{/if}
				{#if errors?.files}
					<p class="error-text">
						{errors?.files}
					</p>
				{/if}
			</div>
		</div>
		<div class="modal-footer">
			<div class="actions">
				<button class="btn btn-danger" type="button" onclick={() => onClose()}>Atšaukti</button>
				<button class="btn btn-primary" type="button" onclick={() => submit()}>
					{submitButtonText ?? (mode === 'create' ? 'Sukurti' : 'Atnaujinti')}
				</button>
			</div>
		</div>
	</div>
</div>

<style>
	:root {
		--panel: #0e1222;
		--panel-2: #111735;
		--text: #eef2ff;
		--muted: rgba(238, 242, 255, 0.68);
		--line: rgba(238, 242, 255, 0.1);
		--line-2: rgba(238, 242, 255, 0.14);
		--danger: #ff5a5f;
		--danger-bg: rgba(255, 90, 95, 0.14);
		--danger-line: rgba(255, 90, 95, 0.32);
		--r-1: 18px;
		--shadow: 0 20px 65px rgba(0, 0, 0, 0.7);
		--glow: 0 0 0 4px rgba(91, 108, 255, 0.16);
	}

	.overlay {
		background: rgba(15, 23, 42, 0.45);
	}

	.modal {
		width: 550px;
		border-radius: var(--r-1);
		border: 1px solid var(--line-2);
		margin: 1rem;
		background:
			linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03)),
			radial-gradient(900px 400px at 50% 0%, rgba(91, 108, 255, 0.14), transparent 55%),
			var(--panel);
		box-shadow: var(--shadow);
		overflow: hidden;
	}

	.modal-header {
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		padding: 18px 20px;
		border-bottom: 1px solid var(--line);
		background: rgba(10, 14, 30, 0.4);
	}

	.header-left {
		display: flex;
		align-items: center;
		gap: 12px;
		min-width: 0;
	}

	.cloud {
		color: #ff4148;
		height: fit-content;
	}

	.header-text {
		color: #fff;
	}

	.header-text h1 {
		font-size: 1.3rem;
		font-weight: 500;
	}

	.header-text p {
		font-size: 0.8rem;
		font-weight: 200;
		margin-top: 3px;
		opacity: 0.8;
	}

	.header-right button {
		display: flex;
		justify-content: center;
		align-items: center;
		width: 35px;
		height: 35px;
		border-radius: 10px;
		border: 1px solid var(--line);
		background: rgba(255, 255, 255, 0.04);
		color: var(--text);
		cursor: pointer;
	}

	.header-right button:hover {
		border-color: rgba(238, 242, 255, 0.18);
	}

	.modal-body {
		max-height: 609px;
		display: flex;
		flex-direction: column;
		gap: 0.8rem;
		padding: 12px 20px 10px;
		overflow-y: auto;
		min-height: 0;
	}

	.card {
		padding: 5px;
	}

	.check-block {
		display: flex;
	}

	.segmented {
		width: 100%;
		padding: 6px 3px;
		border-radius: 7px;
		background: rgba(0, 0, 0, 0.14);
		border: 1px solid transparent;
	}

	.segmented label {
		width: 100%;
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 10px;
		border-radius: 7px;
		font-weight: 700;
		font-size: 0.8rem;
		color: var(--text);
	}

	.card h2 {
		font-weight: 500;
		font-size: 1rem;
		text-align: left;
		color: var(--text);
		margin-bottom: 5px;
	}

	.conditions .field label {
		display: flex;
		margin: 0;
		padding: 0;
	}

	.conditions .field input:not(input[id='release-datetime']) {
		padding: 0;
		border-radius: 0;
	}

	.conditions .field input {
		width: initial;
	}

	.field.search label {
		position: absolute;
		left: 0.8rem;
		top: 50%;
		transform: translateY(-50%);
		display: flex;
		color: rgba(238, 242, 255, 0.55);
		pointer-events: none;
		margin: 0;
	}

	.field.search input {
		padding-left: 2.6rem;
	}

	.dropdown {
		position: absolute;
		top: 100%;
		left: 0;
		right: 0;
		max-height: 220px;
		border-radius: 7px;
		background-color: #171a27;
		color: #d8d8d8;
		font-size: 0.8rem;
		padding: 10px;
		max-height: 75px;
		overflow-y: auto;
		z-index: 50;
	}

	.italic {
		font-style: italic;
	}

	.dropdown-item {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		width: 100%;
		padding: 0.5rem 0.7rem;
		border-radius: 7px;
		border: none;
		background: transparent;
		color: #d4d4d4;
		font-size: 0.8rem;
	}

	.dropdown-item .created-date-time.active {
		font-size: 0.7rem;
		opacity: 0.6;
	}

	.dropdown-item[role='button'] {
		cursor: pointer;
	}

	.selected-rows {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0.5rem;
		padding: 10px 10px;
	}

	.selected-rows .row {
		border: 1px solid transparent;
		border-radius: 7px;
		padding: 0.5rem 0.7rem;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
	}

	.selected-rows.single {
		grid-template-columns: 1fr;
	}

	.selected-rows span {
		font-size: 0.8rem;
		color: #d8d8d8;
	}

	.selected-rows .created-date-time {
		font-size: 0.75rem;
	}

	.selected-rows button {
		display: flex;
		align-items: center;
		background-color: transparent;
		border: none;
		color: #d8d8d8;
	}

	/*take every 4th item (4n) starting from 1*/
	.selected-rows .row:nth-child(4n + 1) {
		border-color: #3b82f6;
		background: rgba(59, 130, 246, 0.12);
	}

	.selected-rows .row:nth-child(4n + 2) {
		border-color: #8b5cf6;
		background: rgba(139, 92, 246, 0.12);
	}

	.selected-rows .row:nth-child(4n + 3) {
		border-color: #14b8a6;
		background: rgba(20, 184, 166, 0.12);
	}

	.selected-rows .row:nth-child(4n + 4) {
		border-color: #f59e0b;
		background: rgba(245, 158, 11, 0.12);
	}

	/* .field {
		margin-top: 10px;
	} */

	.files-filter {
		position: relative;
		width: 100%;
		display: flex;
		align-items: end;
		gap: 0.7rem;
	}

	.files-filter .field {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.files-filter .field label {
		text-align: left;
		margin: 0 0 0 7px;
	}

	.dash-wrap {
		display: flex;
		align-items: center;
		justify-content: center;
		padding-bottom: 20px;
	}

	.dash {
		width: 15px;
		height: 2px;
		border-radius: 999px;
		background: #7f86a8;
	}

	:is(
		.field input[type='date'],
		.field input[type='datetime-local']
	)::-webkit-calendar-picker-indicator {
		opacity: 0.75;
		cursor: pointer;
		filter: invert(1);
	}

	:is(
			.field input[type='date'],
			.field input[type='datetime-local']
		)::-webkit-calendar-picker-indicator:hover {
		opacity: 1;
	}

	.icon-btn {
		background-color: transparent;
		border: none;
		color: #d8d8d8;
		display: flex;
		margin-left: auto;
	}

	.btn {
		padding: 10px 12px;
		border-radius: 12px;
		border: 1px solid var(--line);
		background: rgba(255, 255, 255, 0.04);
		color: var(--text);
		font-weight: 600;
		font-size: 0.8rem;
		cursor: pointer;
		white-space: nowrap;
	}

	button:hover {
		border-color: rgba(238, 242, 255, 0.18);
		background: rgba(255, 255, 255, 0.06);
	}

	.btn-primary {
		border-color: transparent;
		background-color: #3c4cffa9;
	}

	.btn-primary:hover {
		filter: brightness(1.04);
	}

	.btn-danger {
		border-color: var(--danger-line);
		background: var(--danger-bg);
		color: #ffe7e8;
	}

	.segmented:has(.check-block input:checked) {
		background: rgba(91, 108, 255, 0.16);
		border: 1px solid rgba(91, 108, 255, 0.38);
		color: rgba(238, 242, 255, 0.95);
		box-shadow: var(--glow);
	}

	.field label {
		display: block;
		font-size: 12px;
		color: var(--muted);
		margin: 0 0 7px;
	}

	.modal-footer {
		display: flex;
		justify-content: end;
		padding: 14px 20px 18px;
		border-top: 1px solid var(--line);
		background: rgba(10, 14, 30, 0.4);
	}

	.actions {
		display: flex;
		gap: 0.6rem;
		flex-wrap: wrap;
	}

	.field {
		width: 100%;
		position: relative;
	}

	.field input,
	.field select {
		width: 100%;
		padding: 0.6rem;
		border-radius: 7px;
		border: 1px solid var(--line);
		background: rgba(0, 0, 0, 0.18);
		color: rgba(238, 242, 255, 0.92);
		outline: none;
	}

	.field input::placeholder {
		color: rgba(238, 242, 255, 0.45);
	}

	.field input:focus {
		border-color: rgba(91, 108, 255, 0.55);
		box-shadow: 0 0 0 4px rgba(91, 108, 255, 0.16);
	}

	.skeleton-line {
		width: 100%;
		display: inline-block;
		height: 10px;
		border-radius: 999px;
		position: relative;
		overflow: hidden;
		background: rgba(0, 0, 0, 0.1);
	}

	.skeleton-line::after {
		content: '';
		position: absolute;
		inset: 0;
		transform: translateX(-100%);
		background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
		animation: shimmer 1.1s infinite;
	}

	@keyframes shimmer {
		100% {
			transform: translateX(100%);
		}
	}

	/* Taking out the number input rows*/
	/* Chrome, Safari, Edge */
	input[type='number']::-webkit-outer-spin-button,
	input[type='number']::-webkit-inner-spin-button {
		-webkit-appearance: none;
		margin: 0;
	}

	/* Firefox */
	input[type='number'] {
		-moz-appearance: textfield;
		appearance: textfield;
	}

	input[type='search']::-webkit-search-cancel-button {
		-webkit-appearance: none;
		appearance: none;
		display: none;
	}

	:is(.modal-body, .dropdown, .selected-rows)::-webkit-scrollbar {
		width: 10px;
	}

	:is(.modal-body, .dropdown, .selected-rows)::-webkit-scrollbar-thumb {
		background: rgba(255, 255, 255, 0.35);
		border-radius: 999px;
		border: 2px solid transparent;
		background-clip: content-box;
	}

	:is(.modal-body, .dropdown, .selected-rows)::-webkit-scrollbar-thumb:hover {
		background: rgba(255, 255, 255, 0.55);
		background-clip: content-box;
	}

	.error-text {
		/* position: absolute; */
		text-align: left;
		min-height: 0.8rem;
		color: red;
		font-size: 10px;
		margin-left: 3px;
		margin-top: 3px;
	}

	select:required:invalid {
		color: rgba(238, 242, 255, 0.45);
	}

	select option {
		color: var(--text);
		background-color: var(--panel);
	}

	select option:focus {
		background-color: var(--panel-2) !important;
	}

	.selected-rows .critical-error {
		background: rgba(239, 68, 68, 0.12);
		color: #f87171;
	}

	.selected-rows .row small {
		padding: 0.3rem;
		border-radius: 5px;
		font-size: 0.5rem;
		font-weight: 600;
		margin-left: auto;
	}

	.selected-rows .warning {
		background: rgba(245, 158, 11, 0.1);
		color: #fbbf24;
	}

	.error-bar {
		display: flex;
		align-items: center;
		border-radius: 7px;
		padding: 0.5rem;
	}

	.error-bar p {
		font-size: 0.7rem;
		margin: 0;
		flex: 1;
		font-weight: 500;
		color: #ffffffdb;
	}

	.error-bar.telegram {
		border: 1px solid rgba(245, 158, 11, 0.45);
		background: rgba(245, 158, 11, 0.1);
	}

	.error-bar.keys {
		border: 1px solid rgba(239, 68, 68, 0.5);
		background: rgba(239, 68, 68, 0.12);
	}

	.error-bar.telegram + .error-bar.keys {
		margin-top: 0.4rem;
	}

	.error-bar button {
		background-color: transparent;
		border: none;
		color: #fff;
		opacity: 0.75;
		cursor: pointer;
	}

	.error-bar.telegram i {
		display: flex;
	}

	.error-bar.telegram i {
		color: #fbbf24;
	}

	.error-bar.keys i {
		color: #f87171;
	}
</style>
