<script lang="ts">
	import { Plus, Search } from 'lucide-svelte';
	import {
		setLoading,
		filterFilesByDate,
		searchUsersByEmail,
		toast,
		decryptMetaForRow,
		submitPolicy,
		getPolicyPage,
		deletePolicy,
		fetchTelegramLinkStatusFromApi,
		getPolicyDetails,
		releasePolicyNow,
		validatePolicyRecipients,
		pausePolicy,
		resumePolicy,
		type Dialogs,
		type FileSearchItem,
		type DialogSelections,
		type PolicyListItem,
		type PolicyDetails,
		type PolicyFile,
		type DecryptPurpose,
		type DecryptableRow,
		type PolicyFormMode,
		type RecipientValidationItem,
		type PolicyStatus
	} from '$lib';
	import { type UserSearchItem } from '$lib';
	import PolicyTable from '$lib/components/PolicyTable.svelte';
	import PolicyFormDialog from '$lib/components/PolicyFormDialog.svelte';
	import SecretPhraseFormR from '$lib/components/SecretPhraseFormR.svelte';
	import SecretPhraseForm from '$lib/components/SecretPhraseFormG.svelte';
	import { onMount } from 'svelte';
	import PolicyDetailsSide from '$lib/components/PolicyDetailsSide.svelte';
	import { createPromptController } from '$lib';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import { goto } from '$app/navigation';
	import TablePagination from '$lib/components/TablePagination.svelte';

	let policyDialogOpen = $state<boolean>(false);
	let deleteDialogOpen = $state<boolean>(false);
	let confirmDialogOpen = $state<boolean>(false);
	let resumeDialogOpen = $state<boolean>(false);
	let policyToResume = $state<string | null>(null);
	let policyToRelease = $state<string | null>(null);
	let policyToDelete = $state<string | null>(null);
	let policyFormMode = $state<PolicyFormMode>('create');
	let initLoading = $state<boolean>(false);
	let initLoadingDetails = $state<boolean>(false);
	let policyListItems = $state<PolicyListItem[]>([]);
	let error = $state<string | null>(null);
	let page = $state<number>(0);
	let size = $state<10 | 20 | 50 | 100>(10);
	let total = $state<number>(0);
	let payload = $state<DialogSelections | null>(null);
	let searchTerm = $state<string>('');
	let filter = $state<PolicyStatus | null>(null);
	function createInitialDialogs(): Dialogs {
		return {
			users: {
				dialogOpen: false,
				initLoading: false,
				searchResults: [],
				selectedItems: []
			},
			files: {
				dialogOpen: false,
				initLoading: false,
				searchResults: [],
				selectedItems: []
			}
		};
	}
	let dialogs = $state<Dialogs>(createInitialDialogs());
	let sideOpen = $state<boolean>(false);
	let policyDetails = $state<PolicyDetails | null>(null);
	let validationResults = $state<RecipientValidationItem[]>([]);
	let submitButtonText = $state<string | null>(null);

	async function loadPolicyPage(refreshPage = false) {
		if (refreshPage) page = 0;
		try {
			initLoading = true;
			const policyPage = await getPolicyPage({ query: searchTerm.trim(), filter, page, size });
			policyListItems = policyPage.policyList;
			total = policyPage.totalElements;
		} catch (e) {
			error = e instanceof Error ? e.message : 'Nepavyko užkrauti puslapio!';
			toast.error(error);
		} finally {
			setTimeout(() => {
				initLoading = false;
			}, 500);
		}
	}

	onMount(() => {
		loadPolicyPage();
		const onClick = (e: MouseEvent) => {
			const root = (e.target as HTMLElement).closest('.side-panel, .form, .modal, .delete-modal');
			if (!root) sideOpen = false;
		};
		document.addEventListener('click', onClick);
		return () => {
			document.removeEventListener('click', onClick);
		};
	});

	async function openPolicyDialog(): Promise<void> {
		try {
			const telegramOk = await ensureTelegramLinked();
			if (!telegramOk) return;

			policyFormMode = 'create';
			policyDialogOpen = true;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko atidaryti formos!');
		}
	}

	async function ensureTelegramLinked(): Promise<boolean> {
		const telegramLinkStatus = (await fetchTelegramLinkStatusFromApi()).telegramLinked;
		if (!telegramLinkStatus) {
			toast.info('Norėdami sukurti politiką, prijunkite Telegram paskyrą.');
			setTimeout(() => {
				goto('/settings');
			}, 1000);
			return false;
		}
		return true;
	}

	function getRecipientValidation(recipientId: string) {
		return validationResults.find((v: RecipientValidationItem) => v.recipientId === recipientId);
	}

	function telegramMissingCount(): number {
		return validationResults.filter((v: RecipientValidationItem) => !v.telegramLinked).length;
	}

	function keysMissingCount(): number {
		return validationResults.filter((v: RecipientValidationItem) => !v.keysGenerated).length;
	}

	async function onSubmit(payload_: DialogSelections) {
		try {
			const telegramOk = await ensureTelegramLinked();
			if (!telegramOk) return;

			validationResults = await validatePolicyRecipients(payload_.recipients);

			if (keysMissingCount() > 0) {
				submitButtonText = null;
				toast.info('Vienas ar keli gavėjai nėra sugeneravę šifravimo raktų!');
				return;
			}

			if (telegramMissingCount() > 0 && submitButtonText !== 'Vis tiek sukurti') {
				submitButtonText = 'Vis tiek sukurti';
				return;
			}

			if (policyFormMode === 'create') {
				await submitPolicy(payload_, { askSecretPhrase: secretPrompt.ask }, 'create');
				toast.success(
					`${payload_.releaseType.charAt(0) + payload_.releaseType.slice(1).toLowerCase().replace('_', ' ')} politika sėkmingai sukurta!`
				);
			} else {
				if (!policyDetails) {
					toast.error('Politikos duomenys dar neįkelti!');
					return;
				}

				await submitPolicy(
					payload_,
					{ askSecretPhrase: secretPrompt.ask },
					'edit',
					policyDetails.policyId
				);

				toast.success('Politika sėkmingai atnaujinta!');
				sideOpen = false;
			}

			onClose();
			loadPolicyPage();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		} finally {
			secretPrompt.close();
			payload = null;
		}
	}

	function onClose() {
		policyDialogOpen = false;
		deleteDialogOpen = false;
		confirmDialogOpen = false;
		resumeDialogOpen = false;
		dialogs.users.searchResults = [];
		dialogs.users.selectedItems = [];
		dialogs.files.searchResults = [];
		dialogs.files.selectedItems = [];
		validationResults = [];
		submitButtonText = null;
	}

	function onResetDialogs() {
		dialogs.users.dialogOpen = false;
		dialogs.files.dialogOpen = false;
		dialogs.users.searchResults = [];
		dialogs.files.searchResults = [];
	}

	const secretPrompt = createPromptController();

	async function onSearchUsers(q: string) {
		const d = dialogs.users;
		try {
			d.dialogOpen = q.trim().length > 0;
			d.initLoading = true;
			d.searchResults = await searchUsersByEmail(q);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko rasti naudotojų!');
		} finally {
			setTimeout(() => (d.initLoading = false), 1000);
		}
	}

	async function onFilterFiles(args: { fileFrom?: string; fileTo?: string }) {
		const d = dialogs.files;

		try {
			d.dialogOpen = !!(args.fileFrom?.trim() || args.fileTo?.trim());
			d.initLoading = true;
			d.searchResults = await filterFilesByDate(args);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko rasti failų!');
		} finally {
			setTimeout(() => (d.initLoading = false), 1000);
		}
	}

	async function onDecryptOne(fileId: string, purpose: DecryptPurpose) {
		let items: DecryptableRow[] =
			purpose === 'create' ? dialogs.files.searchResults : (policyDetails?.policyFiles ?? []);

		if (purpose !== 'create' && !policyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}

		try {
			items = setLoading(items, true, fileId);
			const r = await decryptMetaForRow(fileId, 'crypto', undefined, {
				askSecretPhrase: secretPrompt.ask
			});

			const fileMeta = r.fileMeta;
			(r as any).metaKey = undefined;
			(r as any).metaFromApi = undefined;
			items = items.map((r) =>
				r.id === fileId ? { ...r, fileName: fileMeta.fileName, fileSize: fileMeta.size } : r
			);

			if (purpose === 'create') {
				dialogs.files.searchResults = items as FileSearchItem[];
			} else if (policyDetails) {
				policyDetails.policyFiles = items as PolicyFile[];
			}
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		} finally {
			secretPrompt.close();
			setTimeout(() => {
				items = setLoading(items, false, fileId);
				if (purpose === 'create') {
					dialogs.files.searchResults = items as FileSearchItem[];
				} else if (policyDetails) {
					policyDetails.policyFiles = items as PolicyFile[];
				}
			}, 1000);
		}
	}

	function onHideOne(fileId: string, purpose: DecryptPurpose) {
		let items: DecryptableRow[] =
			purpose === 'create' ? dialogs.files.searchResults : (policyDetails?.policyFiles ?? []);
		if (purpose !== 'create' && !policyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}
		items = items.map((r) =>
			r.id === fileId ? { ...r, fileName: undefined, fileSize: undefined } : r
		);

		if (purpose === 'create') {
			dialogs.files.searchResults = items as FileSearchItem[];
		} else if (policyDetails) {
			policyDetails.policyFiles = items as PolicyFile[];
		}
	}

	function onAdd(purpose: 'user' | 'file', item: UserSearchItem | FileSearchItem): boolean {
		const d = purpose === 'user' ? dialogs.users : dialogs.files;
		if (d.selectedItems.some((si) => si.id === item.id)) return false;
		d.selectedItems = [...d.selectedItems, item] as typeof d.selectedItems;
		d.dialogOpen = false;
		d.searchResults = [];
		return true;
	}

	function onRemove(purpose: 'user' | 'file', userId: string) {
		const d = purpose === 'user' ? dialogs.users : dialogs.files;
		d.selectedItems = d.selectedItems.filter((si) => si.id !== userId) as typeof d.selectedItems;
		if (purpose === 'user') {
			validationResults = validationResults.filter((vr) => vr.recipientId !== userId);
		}
	}

	async function onSelectPolicy(policyId: string) {
		try {
			policyDetails = await getPolicyDetails(policyId);
			sideOpen = initLoadingDetails = true;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko gauti politikos duomenų!');
		} finally {
			setTimeout(() => (initLoadingDetails = false), 500);
		}
	}

	function openEditPolicyDialog(policyId: string) {
		if (!policyDetails) {
			toast.error('Politikos informacija neįkelta!');
			return;
		}

		dialogs = createInitialDialogs();
		policyFormMode = 'edit';
		const selectedRecipients = policyDetails?.policyRecipients.map((r) => ({
			id: r.id,
			email: r.email
		}));

		const selectedFiles = policyDetails?.policyFiles.map((f) => ({
			id: f.id,
			fileName: f.fileName ?? undefined,
			createdAt: f.createdAt
		}));

		dialogs.users.selectedItems = selectedRecipients;
		dialogs.files.selectedItems = selectedFiles;

		policyDialogOpen = true;
	}

	function onDeletePolicy(policyId: string) {
		deleteDialogOpen = true;
		policyToDelete = policyId;
	}

	async function onConfirmDeletePolicy() {
		if (!policyToDelete) {
			toast.error('Nepasirinkta politika ištrynimui!');
			return;
		}
		try {
			await deletePolicy(policyToDelete);
			loadPolicyPage();
			toast.success('Politika sėkmingai ištrinta!');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko ištrinti politikos!');
		} finally {
			sideOpen = deleteDialogOpen = false;
			policyToDelete = null;
		}
	}

	function onReleaseNow(policyId: string) {
		confirmDialogOpen = true;
		policyToRelease = policyId;
	}

	async function onConfirmReleaseNow() {
		if (!policyToRelease) {
			toast.error('Nepasirinkta politika atskleidimui!');
			return;
		}
		try {
			await releasePolicyNow(policyToRelease);
			toast.success('Politika sėkmingai atskleista!');
			loadPolicyPage();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepaviko rankiniu budu atsklesiti politikos!');
		} finally {
			sideOpen = confirmDialogOpen = false;
			policyToRelease = null;
		}
	}

	async function onPausePolicy(policyId: string) {
		try {
			await pausePolicy(policyId);
			toast.success('Politika sėkmingai pristabdyta!');
			loadPolicyPage();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko pristabdyti politikos!');
		} finally {
			sideOpen = false;
		}
	}

	function onResumePolicy(policyId: string) {
		policyToResume = policyId;
		if (policyDetails?.releaseType === 'DATE_TIME') {
			resumeDialogOpen = true;
			return;
		}
		onConfirmResumePolicy();
	}

	async function onConfirmResumePolicy() {
		if (!policyToResume) {
			toast.error('Nepasirinkta politika pratęsimui!');
			return;
		}
		try {
			await resumePolicy(policyToResume);
			toast.success('Politika sėkmingai pratęsta!');
			loadPolicyPage();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko pratęsti politikos!');
		} finally {
			sideOpen = resumeDialogOpen = false;
			policyToResume = null;
		}
	}

	function onSizeChange(newSize: 10 | 20 | 50 | 100) {
		size = newSize;
		page = 0;
		loadPolicyPage();
	}

	function onPageChange(newPage: number) {
		page = newPage;
		loadPolicyPage();
	}

	async function onFilterClick(selectedFilter: PolicyStatus | null) {
		filter = selectedFilter;
		await loadPolicyPage(true);
	}

	function onClosePolicyDetailsDialiog() {
		sideOpen = false;
	}
</script>

<svelte:head>
	<title>Sąlyginis atskleidimas</title>
</svelte:head>

<div class="conditional-release">
	<div class="header-wrapper">
		<div class="header">
			<h2>Sąlyginis atskleidimas</h2>
			<p>
				Tvarkykite taisykles, kurios automatiškai atskleidžia užšifruotus failus nurodytiems
				gavėjams pagal konkrečias sąlygas.
			</p>
		</div>
		<button type="button"><Plus size={20} onclick={openPolicyDialog} />Kurti politiką</button>
	</div>
	<div class="body">
		<div class="search-sorting">
			<div class="search">
				<button type="button" aria-label="Search"><Search size={20} /></button>
				<input
					type="text"
					placeholder="Ieškoti pagal pavadinimą..."
					bind:value={searchTerm}
					oninput={() => loadPolicyPage(true)}
				/>
			</div>
			<div class="table-sorting-btns">
				<button class:active={filter === null} onclick={() => onFilterClick(null)}>Visos</button>
				<button class:active={filter === 'ACTIVE'} onclick={() => onFilterClick('ACTIVE')}
					>Aktyvios</button
				>
				<button class:active={filter === 'IN_GRACE'} onclick={() => onFilterClick('IN_GRACE')}
					>Įspėjimo laikotarpis</button
				>
				<button class:active={filter === 'RELEASED'} onclick={() => onFilterClick('RELEASED')}
					>Atskleistos</button
				>
				<button class:active={filter === 'PAUSED'} onclick={() => onFilterClick('PAUSED')}
					>Pristabdytos</button
				>
			</div>
		</div>
		<PolicyTable {policyListItems} {initLoading} {error} {searchTerm} {onSelectPolicy} />
		<TablePagination {page} {size} {total} {onSizeChange} {onPageChange} />
	</div>
	<div class="side-panel" class:open={sideOpen}>
		<PolicyDetailsSide
			{policyDetails}
			initLoading={initLoadingDetails}
			{onDecryptOne}
			{onHideOne}
			{openEditPolicyDialog}
			{onDeletePolicy}
			{onReleaseNow}
			{onPausePolicy}
			{onResumePolicy}
			{onClosePolicyDetailsDialiog}
		/>
	</div>
	{#if policyDialogOpen}
		<PolicyFormDialog
			mode={policyFormMode}
			{dialogs}
			{payload}
			{policyDetails}
			{validationResults}
			{getRecipientValidation}
			{telegramMissingCount}
			{keysMissingCount}
			{submitButtonText}
			{onResetDialogs}
			{onSearchUsers}
			{onFilterFiles}
			{onDecryptOne}
			{onHideOne}
			{onAdd}
			{onRemove}
			{onSubmit}
			{onClose}
		/>
	{/if}
	{#if deleteDialogOpen}
		<ConfirmDialog
			title="Ištrinti politiką"
			message="Šio veiksmo nebus galima atšaukti. Ar tikrai norite tęsti?"
			cancelLabel="Atšaukti"
			confirmLabel="Ištrinti"
			{onClose}
			onConfirm={onConfirmDeletePolicy}
		/>
	{/if}
	{#if confirmDialogOpen}
		<ConfirmDialog
			title="Atskleisti politiką dabar"
			message="Ar atskleisti šią politiką dabar? Visi failai bus išsiųsti iš karto."
			cancelLabel="Atšaukti"
			confirmLabel="Atskleisti dabar"
			{onClose}
			onConfirm={onConfirmReleaseNow}
		/>
	{/if}
	{#if resumeDialogOpen}
		<ConfirmDialog
			title="Pratęsti politiką"
			message={policyDetails?.releaseType === 'DATE_TIME' &&
			new Date(policyDetails.scheduledReleaseAt) <= new Date()
				? 'Šios politikos suplanuotas atskleidimo laikas jau praėjo. Jei ją pratęsite, failai gali būti atskleisti iš karto!'
				: 'Ši politika vėl taps aktyvi ir bus atskleista suplanuotu atskleidimo laiku.'}
			cancelLabel="Atšaukti"
			confirmLabel="Pratęsti"
			{onClose}
			onConfirm={onConfirmResumePolicy}
		/>
	{/if}
	{#if secretPrompt.open}
		<SecretPhraseFormR
			onSubmit={secretPrompt.submit}
			onClose={secretPrompt.close}
			loading={secretPrompt.loading}
		/>
	{/if}
</div>
<SecretPhraseForm />

<style>
	.conditional-release {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	.header-wrapper {
		display: flex;
		justify-content: space-between;
		background-color: #fff;
		padding: 10px 20px;
		border-bottom: 1px solid var(--border-cl);
	}

	.header-wrapper button {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0rem 0.8rem;
		border: 1px solid #ddd;
		border-radius: 7px;
		background-color: white;
		color: #222;
		font-size: 0.8rem;
		font-weight: 500;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			border-color 0.15s ease,
			transform 0.1s ease;
	}

	.header-wrapper button:hover {
		background-color: #f7f7f7;
		border-color: #cfcfcf;
	}

	.header-wrapper button:active {
		transform: scale(0.98);
	}

	.header p {
		margin-top: 0.2rem;
		font-size: 0.8rem;
		opacity: 0.7;
	}

	.body {
		background-color: #fff;
		padding: 1rem;
	}

	@media (max-width: 1200px) {
	}

	@media (max-width: 768px) {
	}

	@media (max-width: 600px) {
		.conditional-release {
			margin-top: 1rem;
		}
	}
</style>
