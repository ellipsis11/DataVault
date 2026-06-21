<script lang="ts">
	import sodium from 'libsodium-wrappers-sumo';
	import FileUploader from '$lib/components/FileUploader.svelte';
	import SecretPhraseForm from '$lib/components/SecretPhraseFormG.svelte';
	import SecretPhraseFormR from '$lib/components/SecretPhraseFormR.svelte';
	import FileTable from '$lib/components/FileTable.svelte';
	import { File as FileIcon, Ellipsis } from 'lucide-svelte';
	import {
		toast,
		fetchFilesMetaSummary,
		type FileRow,
		decryptMetaForRow,
		decryptAllMeta,
		fetchEncryptedFileBlob,
		decryptFileBlob,
		hideAllRowsMeta,
		hideRowMeta,
		patchRowsWithMetas,
		patchRowWithMeta,
		setLoading,
		saveBlob,
		saveFileName,
		fileDelete,
		createPromptController,
		uploadOneFile,
		type StorageType
	} from '$lib';
	import { onMount } from 'svelte';
	import FileRenameForm from '$lib/components/FileRenameForm.svelte';
	import FileUploadDialiog from '$lib/components/FileUploadDialiog.svelte';
	import { DecryptionFailedError } from '$lib/error';
	import TablePagination from '$lib/components/TablePagination.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

	let initLoading = $state<boolean>(false);
	let files = $state<File[]>([]);
	let fileRows = $state<FileRow[]>([]);
	let error = $state<string | null>(null);
	let vaultStatus = $state<'initialized' | 'not_initialized' | undefined>();
	let oldFileName = $state<string>('');
	let mimeType = $state<string>('');
	let page = $state<number>(0);
	let size = $state<10 | 20 | 50 | 100>(10);
	let total = $state<number>(0);
	let metaDecrypted = $state<boolean>(false);
	let storageProvider = $state<StorageType>('AWS_S3');
	let fileToDelete = $state<string | null>(null);
	let deleteDialogOpen = $state<boolean>(false);

	const secretPrompt = createPromptController();
	const renamePrompt = createPromptController();

	async function loadFilesPage() {
		try {
			files = [];
			initLoading = true;
			const filePage = await fetchFilesMetaSummary(page, size);
			fileRows = filePage.fileMetaSummary;
			total = filePage.totalElements;
		} catch (e) {
			error = e instanceof Error ? e.message : 'Nepavyko užkrauti saugyklos puslapio!';

			toast.error(error);
		} finally {
			setTimeout(() => (initLoading = false), 1000);
		}
	}

	onMount(loadFilesPage);

	function clearFiles() {
		files = [];
	}

	async function onDecryptOne(fileId: string) {
		try {
			fileRows = setLoading(fileRows, true, fileId);
			const { metaFromApi, fileMeta } = await decryptMetaForRow(fileId, 'full', undefined, {
				askSecretPhrase: secretPrompt.ask
			});

			fileRows = patchRowWithMeta(fileRows, fileId, fileMeta, metaFromApi);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		} finally {
			secretPrompt.close();
			setTimeout(() => {
				fileRows = setLoading(fileRows, false, fileId);
			}, 500);
		}
	}

	async function onDecryptAll() {
		try {
			fileRows = setLoading(fileRows, true);
			const { metasFromApi, fileMetaWithId } = await decryptAllMeta(page, size, {
				askSecretPhrase: secretPrompt.ask
			});
			fileRows = patchRowsWithMetas(fileRows, fileMetaWithId, metasFromApi);
			total = metasFromApi.totalElements;
			metaDecrypted = true;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		} finally {
			secretPrompt.close();
			fileRows = setLoading(fileRows, false);
		}
	}

	function onHideOne(fileId: string) {
		fileRows = hideRowMeta(fileRows, fileId);
	}

	function onHideAll() {
		fileRows = hideAllRowsMeta(fileRows);
		metaDecrypted = false;
	}

	async function onDownload(fileId: string) {
		try {
			const ecnBlob = await fetchEncryptedFileBlob(fileId);
			const { blob, fileMeta } = await decryptFileBlob(fileId, ecnBlob, {
				askSecretPhrase: secretPrompt.ask
			});
			saveBlob(blob, fileMeta.fileName);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko atsisiųsti failo!');
		} finally {
			secretPrompt.close();
		}
	}

	async function onRename(fileId: string) {
		let metaKey: Uint8Array | undefined;

		try {
			const { userId, metaKey, fileMeta } = await decryptMetaForRow(fileId, 'crypto', undefined, {
				askSecretPhrase: secretPrompt.ask
			});

			secretPrompt.close();
			oldFileName = fileMeta.fileName;
			mimeType = fileMeta.mimeType.split('/')[1];

			let newfileName = await renamePrompt.ask();
			if (!newfileName) return;

			fileRows = setLoading(fileRows, true, fileId);
			newfileName = await saveFileName(userId, metaKey, fileId, newfileName, fileMeta);

			fileRows = fileRows.map((f) =>
				f.id === fileId
					? {
							...f,
							fileMeta: f.fileMeta ? { ...f.fileMeta, fileName: newfileName } : f.fileMeta
						}
					: f
			);

			renamePrompt.close();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko pervadinti failo!');
		} finally {
			if (metaKey) sodium.memzero(metaKey);
			renamePrompt.close();
			setTimeout(() => (fileRows = setLoading(fileRows, false, fileId)), 1000);
		}
	}

	function onDelete(fileId: string) {
		deleteDialogOpen = true;
		fileToDelete = fileId;
	}

	async function onConfirmDeleteFile() {
		if (!fileToDelete) {
			toast.error('Nepasirinktas failas ištrynimui!');
			return;
		}

		try {
			fileRows = setLoading(fileRows, true);
			await fileDelete(fileToDelete);

			loadFilesPage();
			toast.success('Failas sėkmingai ištrintas');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Ištrinti nepavyko!');
		} finally {
			deleteDialogOpen = false;
			fileToDelete = null;
		}
	}

	function onClose() {
		deleteDialogOpen = false;
		fileToDelete = null;
	}

	async function onFileUpload() {
		if (files.length === 0) return;

		try {
			const secretPhrase = await secretPrompt.ask();

			for (const file of files) {
				await uploadOneFile(file, secretPhrase, storageProvider);
			}

			toast.success(files.length === 1 ? 'Failas sėkmingai įkeltas.' : 'Failai sėkmingai įkelti.');
			loadFilesPage();
		} catch (e) {
			if (e instanceof DecryptionFailedError) {
				toast.error(e.message);
				return;
			}
			toast.error(e instanceof Error ? e.message : 'Nepavyko užkelti!');
		} finally {
			secretPrompt.close();
		}
	}

	function onDeleteFileFromUpload(fileIndex: number) {
		files = files.filter((_, i) => i !== fileIndex);
	}

	function onChangeStorageProvider(selectedProvider: StorageType) {
		storageProvider = selectedProvider;
	}

	function onSizeChange(newSize: 10 | 20 | 50 | 100) {
		size = newSize;
		page = 0;
		loadFilesPage();
	}

	function onPageChange(newPage: number) {
		page = newPage;
		loadFilesPage();
	}
</script>

<svelte:head>
	<title>Failų saugykla</title>
</svelte:head>

<div class="file-storage">
	<div class="header">
		<h2>Failų saugykla</h2>
		<p>
			Tvarkykite savo užšifruotus failus, peržiūrėkite jų informaciją ir atsisiųskite juos pagal
			poreikį.
		</p>
	</div>
	<div class="body">
		<div class="block-2">
			<FileUploader
				onFiles={(f, status) => {
					files = f;
					vaultStatus = status;
				}}
			/>
			{#if files.length > 0 && vaultStatus === 'initialized'}
				<FileUploadDialiog
					{files}
					onClose={clearFiles}
					onConfirm={onFileUpload}
					{onDeleteFileFromUpload}
					{loadFilesPage}
					{onChangeStorageProvider}
				/>
			{:else if files.length > 0 && vaultStatus === 'not_initialized'}
				<SecretPhraseForm />
			{/if}
			<div class="table-top">
				{#if fileRows.length > 1}
					<div class="action-btns">
						{#if !metaDecrypted}
							<button type="button" onclick={() => onDecryptAll()}
								>Iššifruoti visus metaduomenis</button
							>
						{:else}
							<button type="button" onclick={() => onHideAll()}>Slėpti visus metaduomenis</button>
						{/if}
					</div>
				{/if}
			</div>
			<FileTable
				{fileRows}
				{initLoading}
				{error}
				{onDecryptOne}
				{onHideOne}
				{onDownload}
				{onRename}
				{onDelete}
			/>
			<TablePagination {size} {page} {total} {onSizeChange} {onPageChange} />
		</div>
		<aside class="block-3">
			<div class="panel-head">
				<h2>Image.jpg</h2>
				<button type="button" aria-label="More"><Ellipsis /></button>
			</div>
			<div class="preview">
				<img src="https://picsum.photos/640/360" alt="Preview" />
			</div>
			<div class="meta">
				<div class="meta-row">
					<span class="text">Image</span>
				</div>
				<div class="meta-row">
					<span class="text">1.2 MB</span>
				</div>
			</div>
			<hr />
			<div class="details">
				<div class="d-row">
					<span class="d-text">Uploaded by <b>You</b></span>
				</div>
				<div class="d-row">
					<span class="d-text">SHA 255, 02ec2_599c5</span>
				</div>
				<div class="d-row">
					<span class="d-text">Last sdc: Apr 24 10:02 AM</span>
				</div>
			</div>
			<hr />
			<div class="access">
				<span class="d-text"><b>Private</b></span>
			</div>
			<hr />
			<div class="actions">
				<button class="btn primary" type="button">Download </button>
				<button class="btn" type="button">Rename </button>
				<button class="btn icon" type="button" aria-label="More actions"><Ellipsis /></button>
				<button class="btn" type="button">Move </button>
				<button class="btn" type="button">Delete </button>
			</div>
		</aside>
	</div>
	{#if secretPrompt.open}
		<SecretPhraseFormR
			onSubmit={secretPrompt.submit}
			onClose={secretPrompt.close}
			loading={secretPrompt.loading}
		/>
	{/if}
	{#if renamePrompt.open}
		<FileRenameForm
			{oldFileName}
			{mimeType}
			onSubmit={renamePrompt.submit}
			onClose={renamePrompt.close}
			loading={renamePrompt.loading}
		/>
	{/if}
	{#if deleteDialogOpen}
		<ConfirmDialog
			title="Ištrinti failą"
			message="Šio veiksmo nebus galima atšaukti. Ar tikrai norite tęsti?"
			cancelLabel="Atšaukti"
			confirmLabel="Ištrinti"
			{onClose}
			onConfirm={onConfirmDeleteFile}
		/>
	{/if}
</div>
<SecretPhraseForm />

<style>
	.block-3 {
		display: none;
	}

	.header {
		background-color: #fff;
		padding: 10px 20px;
		border-bottom: 1px solid var(--border-cl);
	}

	.header p {
		margin-top: 0.2rem;
		font-size: 0.8rem;
		opacity: 0.7;
	}

	.block-2 > * {
		margin-top: 1rem;
	}

	.action-btns {
		margin-left: auto;
		margin-bottom: 0.5rem;
	}

	.action-btns button {
		background-color: #fbfbfb50;
		padding: 5px 15px;
		border: 1px solid #cdcdcd;
		border-radius: 7px;
		font-size: 0.9rem;
		cursor: pointer;
	}

	.action-btns button:hover {
		background-color: #e3e3ef;
		border-color: #cdcdcd;
		box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
	}

	.action-btns button:active {
		transform: translateY(-1px);
	}

	.table-top {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.block-3 {
		background: rgba(255, 255, 255, 0.65);
		border: 1px solid #00000012;
		padding: 25px;
	}

	.panel-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 15px;
	}

	.panel-head h2 {
		font-size: 1.6rem;
		font-weight: 600;
	}

	.panel-head button {
		border: none;
		background-color: transparent;
	}

	.preview {
		border-radius: 12px;
		overflow: hidden;
		border: 1px solid #00000012;
		box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
	}

	.preview img {
		display: block;
		width: 100%;
		max-height: 180px;
		object-fit: cover;
	}

	.meta {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		padding: 14px 2px 6px;
	}

	.meta-row {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.block-3 hr {
		border: none;
		height: 1px;
		background: rgba(229, 231, 235, 0.9);
		margin: 10px 0;
	}

	.details {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		padding: 4px 2px;
		color: #374151;
	}
	.d-row,
	.access {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.d-text {
		font-size: 16px;
		line-height: 1.25;
	}

	.copy-btn {
		margin-left: auto;
		border-radius: 7px;
		border: 1px solid #00000012;
		background: transparent;
	}

	.access {
		padding: 6px 2px;
		color: #111827;
	}

	.btn {
		border-radius: 7px;
		border: 1px solid rgba(229, 231, 235, 0.9);
		background: rgba(255, 255, 255, 0.75);
	}

	@media (max-width: 1200px) {
	}

	@media (max-width: 768px) {
	}

	@media (max-width: 600px) {
		.file-storage {
			margin-top: 1rem;
		}
	}
</style>
