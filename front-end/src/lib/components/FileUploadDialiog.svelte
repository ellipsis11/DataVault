<script lang="ts">
	import { CloudUpload, Trash2, X, File as FileI, Volume } from 'lucide-svelte';
	import { toast, sha256, convertFileSize, type StorageType } from '$lib';
	import { formatDate } from '$lib';

	type Props = {
		files: File[];
		onClose: () => void;
		onConfirm: () => void;
		onDeleteFileFromUpload: (fileIndex: number) => void;
		loadFilesPage: () => void;
		onChangeStorageProvider: (storageProvider: StorageType) => void;
	};

	let {
		files = [],
		onClose,
		onConfirm,
		onDeleteFileFromUpload,
		onChangeStorageProvider
	}: Props = $props();
	let checksums = $state<Record<number, string>>({});
	let copiedId = $state<number | null>(null);
	let storageProvider = $state<StorageType>('AWS_S3');

	$effect(() => {
		if (!files.length) {
			checksums = {};
			return;
		}

		(async () => {
			const next: Record<number, string> = {};

			for (const [i, file] of files.entries()) {
				const h = await sha256(file);
				next[i] = h;
			}

			checksums = next;
		})();
	});

	let ext = (file: File): string => {
		const i = file.name.lastIndexOf('.');
		return i >= 0 ? file.name.slice(i + 1).toLocaleUpperCase() : '';
	};

	const shortHash = (h: string) => (h ? `${h.slice(0, 8)}...${h.slice(-8)}` : '-');

	async function copyChecksum(checksum: string, fileId = 0) {
		try {
			await navigator.clipboard.writeText(checksum);
			copiedId = fileId;
			setTimeout(() => {
				copiedId = null;
			}, 2000);
		} catch {
			toast.error('Nepavyko nukopijuoti kontrolinės sumos!');
		}
	}

	function changeStorageProvider(selectedProvider: StorageType) {
		storageProvider = selectedProvider;
		onChangeStorageProvider(storageProvider);
	}
</script>

<div class="overlay">
	<div class="modal">
		<div class="modal-header">
			<div class="header-left">
				<span class="cloud"><CloudUpload size={34} /></span>
				<div class="header-text">
					<h1>Failų įkėlimas</h1>
					<p>Įkelkite failus ir pasirinkite jų saugojimo vietą.</p>
				</div>
			</div>
			<div class="header-right">
				<button type="button" onclick={onClose}><X /></button>
			</div>
		</div>
		<div class="modal-body">
			{#if files.length === 1}
				<div class="card file">
					<div class="meta">
						<div class="file-badge" data-ext={ext(files[0])}></div>
						<div class="file-meta">
							<h3>{files[0].name}</h3>
							<div class="file-row">
								<span class="dt">•</span>
								<span>{convertFileSize(files[0].size)}</span><span class="dt">•</span>
								<span>Pakeista: {formatDate(files[0].lastModified)}</span>
							</div>
						</div>
					</div>
					<div class="checksum-row">
						<div class="chip">
							<b>SHA-256:</b>
							<span class="hash">{checksums[0] ? shortHash(checksums[0]) : 'Skaičiuojama...'}</span>
						</div>
						<button class="btn" onclick={() => copyChecksum(checksums[0])}
							>{copiedId === 0 ? 'Nukopijuota ✓' : 'Kopijuoti'}</button
						>
					</div>
				</div>
			{:else}
				<div class="card files">
					{#each files as file, i}
						<div class="file-item">
							<div class="file-badge" data-ext={ext(file)}></div>
							<div class="meta">
								<div class="file-meta">
									<h3>{file.name}</h3>
									<div class="file-row">
										<span class="dt">•</span>
										<span>{convertFileSize(file.size)}</span><span class="dt">•</span>
										<span>Pakeista: {formatDate(file.lastModified)}</span>
									</div>
								</div>
								<div class="checksum-row">
									<div class="chip">
										<b>SHA-256:</b>
										<span class="hash"
											>{checksums[i] ? shortHash(checksums[i]) : 'Skaičiuojama...'}</span
										>
									</div>
									<button class="btn" onclick={() => copyChecksum(checksums[i], i)}
										>{copiedId === i ? 'Nukopijuota ✓' : 'Kopijuoti'}</button
									>
								</div>
							</div>
							<button class="delete-btn" onclick={() => onDeleteFileFromUpload(i)}
								><Trash2 size={12} /> Pašalinti</button
							>
						</div>
					{/each}
				</div>
				<div class="file-upload-sum">
					<div>
						<FileI size={15} />
						<span>Viso: {files.length} failai</span>
					</div>
					<span
						>Bendras dydis: {convertFileSize(
							files.reduce((total, file) => total + file.size, 0)
						)}</span
					>
				</div>
			{/if}
			<div class="card storage">
				<h2>Kur saugoti?</h2>
				<div class="storage-selection">
					<div class="segmented">
						<input
							type="radio"
							id="aws"
							checked={storageProvider === 'AWS_S3'}
							onclick={() => changeStorageProvider('AWS_S3')}
						/>
						<label for="aws">
							<img src="/img/aws-s3-logo.png" alt="aws" width="20" height="20" />
							AWS (S3)<span class="dot"></span>
						</label>
						<input
							type="radio"
							id="ipfs"
							checked={storageProvider === 'FILEBASE_IPFS'}
							onclick={() => changeStorageProvider('FILEBASE_IPFS')}
						/>
						<label for="ipfs">
							<img src="/img/filebase-logo.png" alt="filebase" width="20" height="20" />
							Filebase (IPFS)<span class="dot"></span>
						</label>
					</div>
				</div>
			</div>
		</div>
		<div class="modal-footer">
			<div class="actions">
				<button class="btn btn-danger" type="button" onclick={onClose}>Atšaukti</button>
				<button class="btn btn-primary" type="button" onclick={onConfirm}>Įkelti</button>
			</div>
		</div>
	</div>
</div>

<style>
	:root {
		--panel: #0e1222;
		--text: #eef2ff;
		--muted: rgba(238, 242, 255, 0.68);
		--line: rgba(238, 242, 255, 0.1);
		--line-2: rgba(238, 242, 255, 0.14);
		--danger: #ff5a5f;
		--danger-bg: rgba(255, 90, 95, 0.14);
		--danger-line: rgba(255, 90, 95, 0.32);
		--r-1: 18px;
		--r-2: 14px;
		--r-3: 12px;
		--shadow: 0 20px 65px rgba(0, 0, 0, 0.7);
		--glow: 0 0 0 4px rgba(91, 108, 255, 0.16);
	}

	.overlay {
		background: rgba(15, 23, 42, 0.45);
	}

	.modal {
		max-width: 500px;
		width: 100%;
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
		display: flex;
		flex-direction: column;
		gap: 0.8rem;
		padding: 12px 20px 10px;
	}

	.card {
		padding: 5px;
	}

	.file {
		border-radius: var(--r-2);
		border: 1px solid var(--line);
		background:
			linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02)),
			rgba(10, 14, 30, 0.4);
		padding: 14px;
	}

	.meta {
		display: flex;
		gap: 14px;
		align-items: flex-start;
	}

	.file-badge {
		width: 40px;
		height: 54px;
		border-radius: 7px;
		background: rgba(255, 255, 255, 0.06);
		border: 1px solid var(--line);
		display: grid;
		place-items: center;
		flex-shrink: 0;
		position: relative;
		overflow: hidden;
		clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 0 100%);
	}

	.file-badge::after {
		content: attr(data-ext);
		position: absolute;
		bottom: 7px;
		left: 50%;
		transform: translateX(-50%);
		font-weight: 800;
		font-size: 0.4rem;
		padding: 3px 8px;
		border-radius: 999px;
		background: rgba(255, 90, 95, 0.2);
		border: 1px solid rgba(255, 90, 95, 0.35);
		color: #ffd5d7;
		letter-spacing: 0.5px;
	}

	.file-meta {
		flex: 1;
		min-width: 0;
		margin-top: 5px;
	}

	.file-meta h3 {
		font-size: 1rem;
		font-weight: 500;
		overflow: hidden;
		text-align: left;
		color: var(--text);
		max-width: 100%;
	}

	.file-row {
		margin-top: 6px;
		color: var(--muted);
		font-size: 0.8rem;
		font-weight: 300;
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		align-items: center;
	}

	.file-row .dt {
		font-size: 0.6rem;
	}

	.checksum-row {
		margin-top: 0.7rem;
		display: flex;
		gap: 10px;
		flex-wrap: wrap;
		align-items: center;
		opacity: 0.8;
	}

	.chip {
		display: flex;
		gap: 0.4rem;
		padding: 10px 12px;
		border-radius: 12px;
		border: 1px solid var(--line);
		background: rgba(0, 0, 0, 0.16);
		color: rgba(238, 242, 255, 0.86);
		font-size: 0.8rem;
		min-width: 50%;
	}

	.chip b {
		color: rgba(238, 242, 255, 0.92);
	}

	.hash {
		font-family: monospace;
		white-space: nowrap;
	}

	.card.files {
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
		max-height: 10rem;
		overflow-x: hidden;
		overflow-y: auto;
		padding-right: 0.8rem;
		min-width: 0;
	}

	.card h2 {
		font-weight: 500;
		font-size: 1rem;
		text-align: left;
		color: var(--text);
		margin-bottom: 5px;
	}

	.file-item {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		min-width: 0;
	}

	.file-item .file-badge::after {
		font-size: 0.5rem;
	}

	.file-item .meta {
		flex-direction: column;
		gap: 0;
	}

	.file-item .checksum-row {
		margin-top: 0.3rem;
	}

	.file-item .checksum-row .chip {
		padding: 0.2rem 0.5rem;
		font-size: 0.6rem;
	}

	.file-item .checksum-row .btn {
		padding: 0.2rem 0.5rem;
		font-size: 0.6rem;
		border-radius: 7px;
	}

	.file-item .delete-btn {
		display: flex;
		align-items: center;
		gap: 0.2rem;
		background: none;
		border: none;
		border: 1px solid #f69696;
		padding: 0.4rem;
		border-radius: 7px;
		color: #f69696;
		font-size: 0.6rem;
		margin-left: auto;
	}

	.file-upload-sum {
		display: flex;
		justify-content: space-between;
		align-items: center;
		color: var(--muted);
		font-size: 0.85rem;
		font-weight: 300;
		border-radius: 7px;
	}

	.file-upload-sum div {
		display: flex;
		align-items: center;
		gap: 0.2rem;
	}

	.storage-selection {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		margin-bottom: 10px;
		flex-wrap: wrap;
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

	.segmented {
		width: 100%;
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 6px 3px;
		border-radius: 7px;
		background: rgba(0, 0, 0, 0.14);
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
		cursor: pointer;
		color: var(--text);
		border: 2px solid transparent;
	}

	.dot {
		width: 8px;
		height: 8px;
		border-radius: 999px;
		margin-left: auto;
		background: rgba(238, 242, 255, 0.2);
	}

	input:not(.rf-fields input) {
		display: none;
		position: absolute;
		left: 0;
		pointer-events: none;
	}

	input:checked + label {
		background: rgba(91, 108, 255, 0.16);
		border-color: rgba(91, 108, 255, 0.38);
		color: rgba(238, 242, 255, 0.95);
		box-shadow: var(--glow);
	}

	input:checked + label .dot {
		background: rgba(91, 108, 255, 0.95);
	}

	.modal-footer {
		padding: 0.5rem 1.25rem 0.625rem;
		border-top: 1px solid var(--line);
		background: rgba(10, 14, 30, 0.4);
		display: flex;
		gap: 0.6rem;
		flex-wrap: wrap;
	}

	.actions {
		width: 100%;
		display: flex;
		gap: 0.7rem;
		align-items: center;
		justify-content: flex-end;
	}

	.dt {
		font-size: 0.6rem;
	}

	*::-webkit-scrollbar-button {
		display: none;
		width: 0;
		height: 0;
	}

	*::-webkit-scrollbar {
		width: 5px;
	}

	*::-webkit-scrollbar-track,
	*::-webkit-scrollbar-track-piece {
		background: transparent;
		border: none;
		box-shadow: none;
	}

	*::-webkit-scrollbar-thumb {
		background: #9ca3af;
		border-radius: 999px;
	}

	@media (max-width: 450px) {
		.dt {
			display: none;
		}

		.file-row {
			flex-direction: column;
			align-items: flex-start;
			gap: 0.3rem;
		}

		.checksum-row {
			flex-direction: column;
		}

		.checksum-row .chip,
		.checksum-row .btn {
			width: 100%;
		}

		.checksum-row .chip {
			justify-content: space-between;
		}

		.segmented {
			flex-direction: column;
		}
	}
</style>
