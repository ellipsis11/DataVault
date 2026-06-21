<script lang="ts">
	import { onMount } from 'svelte';
	import { Search, X } from 'lucide-svelte';
	import {
		getReceivedPolicyPage,
		getReceivedPolicyDetails,
		toast,
		setLoading,
		decryptReceivedFileMetaForRow,
		decryptReceivedFileBlob,
		fetchReceivedFileBlob,
		saveBlob,
		saveFilesAsZip,
		type ReceivedPolicyListItem,
		type ReceivedPolicyDetails,
		type DecryptableRow,
		type PolicyFile,
		deleteReceivedPolicyAccess
	} from '$lib';
	import ReceivedPolicyTable from '$lib/components/ReceivedPolicyTable.svelte';
	import { get } from 'svelte/store';
	import { newReceivedPoliciesCount } from '$lib/stores/receivedPolicies';
	import ReceivedPolicyDetailsSide from '$lib/components/ReceivedPolicyDetailsSide.svelte';
	import SecretPhraseForm from '$lib/components/SecretPhraseFormG.svelte';
	import SecretPhraseFormR from '$lib/components/SecretPhraseFormR.svelte';
	import { createPromptController } from '$lib';
	import { loadReceivedPoliciesCounts } from '$lib/stores/receivedPolicies';
	import TablePagination from '$lib/components/TablePagination.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

	type Filter = 'NEW' | 'VIEWED';

	let initLoading = $state<boolean>(false);
	let newPolicyCount = $state<number>(0);
	let receivedPolicyListItems = $state<ReceivedPolicyListItem[]>([]);
	let releaseBannerOpen = $state<boolean>(true);
	let error = $state<string | null>(null);
	let page = $state<number>(0);
	let size = $state<10 | 20 | 50 | 100>(10);
	let total = $state<number>(0);
	let receivedPolicyDetails = $state<ReceivedPolicyDetails | undefined>();
	let sideOpen = $state<boolean>(false);
	let initLoadingDetails = $state<boolean>(false);
	let searchTerm = $state<string>('');
	let filter = $state<Filter | null>(null);
	let deleteDialogOpen = $state<boolean>(false);
	let policyToDelete = $state<string | null>(null);

	const secretPrompt = createPromptController();

	async function loadReceivedPolicyPage(refreshPage = false) {
		initLoading = true;
		if (refreshPage) page = 0;

		try {
			const receivedPolicyPage = await getReceivedPolicyPage({
				query: searchTerm.trim(),
				filter,
				page,
				size
			});
			receivedPolicyListItems = receivedPolicyPage.receivedPolicyList;
			total = receivedPolicyPage.totalElements;
		} catch (e) {
			error = e instanceof Error ? e.message : 'Nepavyko užkrauti gautų politikų puslapio!';
			toast.error(error);
		} finally {
			await delay(500);
			initLoading = false;
		}
	}

	async function handleNewPolicyCountChange() {
		try {
			await loadReceivedPoliciesCounts();
			newPolicyCount = get(newReceivedPoliciesCount);
		} catch (e) {
			error = e instanceof Error ? e.message : 'Įvyko klaida!';
			toast.error(error);
		}
	}

	onMount(() => {
		const onClick = (e: MouseEvent) => {
			const root = (e.target as HTMLElement).closest('.side-panel, .form');
			if (!root) sideOpen = false;
		};

		document.addEventListener('click', onClick);

		(async () => {
			initLoading = true;
			await loadReceivedPolicyPage();
			await handleNewPolicyCountChange();
		})();

		return () => {
			document.removeEventListener('click', onClick);
		};
	});

	async function onSelectPolicy(policyId: string) {
		try {
			await handleNewPolicyCountChange();
			receivedPolicyDetails = await getReceivedPolicyDetails(policyId);
			receivedPolicyListItems = receivedPolicyListItems.map((rp) =>
				rp.policyId === policyId ? { ...rp, viewed: true } : rp
			);
			sideOpen = initLoadingDetails = true;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko gauti politikos informacijos!');
		} finally {
			setTimeout(() => (initLoadingDetails = false), 500);
		}
	}

	async function onDecryptOne(
		fileId: string,
		secretPhrase?: string,
		mode: 'single' | 'bulk' = 'single'
	) {
		if (!receivedPolicyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}

		let items: DecryptableRow[] = receivedPolicyDetails.policyFiles;

		if (mode === 'single') {
			items = setLoading(items, true, fileId);
			receivedPolicyDetails.policyFiles = items as PolicyFile[];
		}

		try {
			const fileMeta = await decryptReceivedFileMetaForRow(
				receivedPolicyDetails.policyId,
				fileId,
				{
					askSecretPhrase: secretPrompt.ask
				},
				secretPhrase
			);

			if (mode === 'bulk') {
				return {
					fileId,
					fileName: fileMeta.fileName,
					fileSize: fileMeta.size
				};
			}

			items = items.map((r) =>
				r.id === fileId ? { ...r, fileName: fileMeta.fileName, fileSize: fileMeta.size } : r
			);

			receivedPolicyDetails.policyFiles = items as PolicyFile[];
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		} finally {
			if (mode === 'single') {
				secretPrompt.close();
				setTimeout(() => {
					items = setLoading(items, false, fileId);
					if (receivedPolicyDetails) {
						receivedPolicyDetails.policyFiles = items as PolicyFile[];
					}
				}, 1000);
			}
		}
	}

	function onHideOne(fileId: string) {
		if (!receivedPolicyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}

		let items: DecryptableRow[] = receivedPolicyDetails.policyFiles;
		items = items.map((r) =>
			r.id === fileId ? { ...r, fileName: undefined, fileSize: undefined } : r
		);

		receivedPolicyDetails.policyFiles = items as PolicyFile[];
	}

	async function onDownload(
		fileId: string,
		secretPhrase?: string,
		mode: 'single' | 'bulk' = 'single'
	) {
		if (!receivedPolicyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}
		try {
			const ecnBlob = await fetchReceivedFileBlob(receivedPolicyDetails.policyId, fileId);
			const { blob, fileMeta } = await decryptReceivedFileBlob(
				receivedPolicyDetails.policyId,
				fileId,
				ecnBlob,
				{
					askSecretPhrase: secretPrompt.ask
				},
				secretPhrase
			);
			if (mode === 'bulk') {
				return {
					fileId,
					blob,
					fileMeta
				};
			}
			saveBlob(blob, fileMeta.fileName);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Atsisiųsti nepavyko!');
		} finally {
			if (mode === 'single') secretPrompt.close();
		}
	}

	async function onDecryptAll() {
		if (!receivedPolicyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}

		receivedPolicyDetails.policyFiles = setLoading(
			receivedPolicyDetails.policyFiles,
			true
		) as PolicyFile[];

		try {
			const secretPhrase = await secretPrompt.ask();
			const results = await Promise.all(
				receivedPolicyDetails.policyFiles
					.filter((file) => !file.fileName)
					.map((file) => onDecryptOne(file.id, secretPhrase, 'bulk'))
			);

			receivedPolicyDetails.policyFiles = receivedPolicyDetails.policyFiles.map((file) => {
				const found = results.find((r) => r?.fileId === file.id);
				return found ? { ...file, fileName: found.fileName, fileSize: found.fileSize } : file;
			});
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		} finally {
			secretPrompt.close();
			setTimeout(() => {
				if (receivedPolicyDetails) {
					receivedPolicyDetails.policyFiles = setLoading(
						receivedPolicyDetails.policyFiles,
						false
					) as PolicyFile[];
				}
			}, 1000);
		}
	}

	function onHideAll() {
		if (!receivedPolicyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}

		receivedPolicyDetails.policyFiles = receivedPolicyDetails.policyFiles.map((file) => ({
			...file,
			fileName: undefined,
			fileSize: undefined
		}));
	}

	async function onDownloadAll() {
		if (!receivedPolicyDetails) {
			toast.error('Politikos failai neįkelti!');
			return;
		}

		try {
			const secretPhrase = await secretPrompt.ask();
			const files = await Promise.all(
				receivedPolicyDetails.policyFiles.map((file) => onDownload(file.id, secretPhrase, 'bulk'))
			);
			const readyFiles = files.filter(Boolean).map((file) => ({
				fileName: file!.fileMeta.fileName,
				blob: file!.blob
			}));

			if (readyFiles.length === 1) {
				saveBlob(readyFiles[0].blob, readyFiles[0].fileName);
				return;
			}
			await saveFilesAsZip(readyFiles, 'gauti-failai.zip');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Atsisiųsti nepavyko!');
		} finally {
			secretPrompt.close();
		}
	}

	function onDelete(policyId: string) {
		deleteDialogOpen = true;
		policyToDelete = policyId;
	}

	async function onConfirmDeletePolicyAccess() {
		if (!policyToDelete) {
			toast.error('Nepasirinkta politika ištrynimui!');
			return;
		}

		try {
			await deleteReceivedPolicyAccess(policyToDelete);
			loadReceivedPolicyPage();

			toast.success('Politika sėkmingai ištrinta!');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko ištrinti politikos!');
		} finally {
			sideOpen = deleteDialogOpen = false;
			policyToDelete = null;
		}
	}

	function onClose() {
		deleteDialogOpen = false;
	}

	const closeReleaseBanner = () => (releaseBannerOpen = false);
	const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

	function onSizeChange(newSize: 10 | 20 | 50 | 100) {
		size = newSize;
		page = 0;
		loadReceivedPolicyPage();
	}

	function onPageChange(newPage: number) {
		page = newPage;
		loadReceivedPolicyPage();
	}

	async function onFilterClick(selectedFilter: Filter | null) {
		filter = selectedFilter;
		await loadReceivedPolicyPage(true);
	}

	function onCloseReceivedPolicyDetailsDialiog() {
		sideOpen = false;
	}
</script>

<svelte:head>
	<title>Gautos politikos</title>
</svelte:head>

<div class="released-policies">
	<div class="header">
		<h2>Man atskleisti failai</h2>
		<p>Peržiūrėkite jums atskleistas politikas, jų failus ir atsisiųskite juos pagal poreikį.</p>
	</div>
	{#if newPolicyCount > 0}
		<div class="release-banner" class:hidden={!releaseBannerOpen}>
			<div class="icon-wrap">
				<span class="icon">i</span>
			</div>
			<div class="content">
				<h3>Atskleisti nauji failai!</h3>
				<p>
					{newPolicyCount === 1
						? `Turite ${newPolicyCount} naują atskleistą politiką, kurią galite peržiūrėti.`
						: `Turite ${newPolicyCount} naujas atskleistas politikas, kurias galite peržiūrėti.`}
				</p>
			</div>
			<button type="button" onclick={closeReleaseBanner}><X size={20} /></button>
		</div>
	{/if}
	<div class="body">
		<div class="search-sorting">
			<div class="search">
				<button type="button" aria-label="Search"><Search size={20} /></button>
				<input
					type="text"
					placeholder="Ieškoti pagal pavadinimą..."
					bind:value={searchTerm}
					oninput={() => loadReceivedPolicyPage(true)}
				/>
			</div>
			<div class="table-sorting-btns">
				<button class:active={filter === null} onclick={() => onFilterClick(null)}>Visos</button>
				<button class:active={filter === 'NEW'} onclick={() => onFilterClick('NEW')}>Naujos</button>
				<button class:active={filter === 'VIEWED'} onclick={() => onFilterClick('VIEWED')}
					>Peržiūrėtos</button
				>
			</div>
		</div>
		<ReceivedPolicyTable
			{receivedPolicyListItems}
			{initLoading}
			{error}
			{searchTerm}
			{onSelectPolicy}
		/>
		<TablePagination {size} {page} {total} {onSizeChange} {onPageChange} />
	</div>
	<div class="side-panel" class:open={sideOpen}>
		<ReceivedPolicyDetailsSide
			policyDetails={receivedPolicyDetails}
			initLoading={initLoadingDetails}
			{onDecryptOne}
			{onHideOne}
			{onDownload}
			{onDecryptAll}
			{onDownloadAll}
			{onDelete}
			{onHideAll}
			{onCloseReceivedPolicyDetailsDialiog}
		/>
	</div>
	{#if secretPrompt.open}
		<SecretPhraseFormR
			onSubmit={secretPrompt.submit}
			onClose={secretPrompt.close}
			loading={secretPrompt.loading}
		/>
	{/if}
	{#if deleteDialogOpen}
		<ConfirmDialog
			title="Ištrinti politiką"
			message="Šio veiksmo nebus galima atšaukti. Ar tikrai norite tęsti?"
			cancelLabel="Atšaukti"
			confirmLabel="Ištrinti"
			{onClose}
			onConfirm={onConfirmDeletePolicyAccess}
		/>
	{/if}
</div>
<SecretPhraseForm />

<style>
	.released-policies {
		display: flex;
		flex-direction: column;
	}

	.header {
		background-color: #fff;
		padding: 10px 20px;
		margin-bottom: 1.5rem;
		border-bottom: 1px solid var(--border-cl);
	}

	.header p {
		margin-top: 0.2rem;
		font-size: 0.8rem;
		opacity: 0.7;
	}

	.release-banner {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 2rem;
		padding: 1.25rem 1.5rem;
		margin-bottom: 1.5rem;
		border-radius: 18px;
		background: linear-gradient(135deg, #eefaf2 0%, #e3f5e8 100%);
		border: 1px solid #cde9d5;
		box-shadow: 0 8px 24px rgba(20, 83, 45, 0.06);
		overflow: hidden;
		max-height: 100px;
		transition:
			opacity 0.2s ease,
			max-height 0.22s ease,
			padding 0.22s ease,
			margin 0.22s ease;
	}

	.release-banner.hidden {
		opacity: 0;
		max-height: 0;
		padding-top: 0;
		padding-bottom: 0;
		margin-bottom: 0;
	}

	.release-banner .icon-wrap {
		display: flex;
		justify-content: center;
		align-items: center;
		width: 2rem;
		height: 2rem;
		border-radius: 999px;
		background: #dff3e5;
	}

	.release-banner .icon {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 1rem;
		height: 1rem;
		border-radius: 999px;
		background: #1f8f4d;
		color: #fff;
		font-weight: 700;
		font-family: sans-serif;
	}

	.release-banner .content {
		flex: 1;
		min-width: 0;
	}

	.release-banner .content h3 {
		margin: 0 0 0.1rem 0;
		font-weight: 700;
		line-height: 1.15;
		color: #163323;
	}

	.release-banner .content p {
		margin: 0;
		font-size: 0.8rem;
		line-height: 1.45;
		color: #446154;
	}

	.release-banner button {
		background-color: transparent;
		border: none;
		transition: transform 0.2s ease;
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
		.released-policies {
			margin-top: 1rem;
		}

		.release-banner {
			position: relative;
			gap: 1rem;
			padding: 0.7rem;
		}

		.release-banner button {
			position: absolute;
			right: 0;
			top: 0;
			margin: 0.7rem;
		}
	}
</style>
