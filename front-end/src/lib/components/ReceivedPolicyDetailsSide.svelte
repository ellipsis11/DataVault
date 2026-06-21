<script lang="ts">
	import { File, Download, Eye, EyeOff, LockOpen, ArrowLeft, Trash2 } from 'lucide-svelte';
	import {
		formatDate,
		convertFileSize,
		shortFileName,
		type ReceivedPolicyDetails,
		translatePolicyType
	} from '$lib';

	type Props = {
		policyDetails: ReceivedPolicyDetails | undefined;
		initLoading: boolean;
		onDecryptOne: (fileId: string) => void;
		onHideOne: (fileId: string) => void;
		onDownload: (fileId: string) => void;
		onDecryptAll: () => void;
		onHideAll: () => void;
		onDownloadAll: () => void;
		onDelete: (policyId: string) => void;
		onCloseReceivedPolicyDetailsDialiog: () => void;
	};

	let {
		policyDetails,
		initLoading,
		onDecryptOne,
		onHideOne,
		onDownload,
		onDecryptAll,
		onHideAll,
		onDownloadAll,
		onDelete,
		onCloseReceivedPolicyDetailsDialiog
	}: Props = $props();
	let openMenuId = $state<string | null>(null);
</script>

<div class="header">
	<div class="go-back-btn">
		<button onclick={onCloseReceivedPolicyDetailsDialiog}
			><ArrowLeft size={18} strokeWidth={1.5} />Atgal</button
		>
	</div>
	<div class="header-upper">
		{#if initLoading}
			<div class="skeleton skeleton-title"></div>
			<div class="skeleton skeleton-pill"></div>
		{:else}
			<h1>{policyDetails?.policyName ?? '—'}</h1>
			<div class="pills">
				<span class="pill releaseType"
					>{policyDetails?.releaseType
						? translatePolicyType(policyDetails?.releaseType ?? '—')
						: '—'}</span
				>
				<span class="pill" data-status={policyDetails?.viewed ? 'VIEWED' : 'NEW'}>
					{policyDetails?.viewed ? 'Peržiūrėta' : 'Nauja'}
				</span>
			</div>
		{/if}
	</div>
</div>
<div class="body">
	{#if initLoading}
		<div class="skeleton skeleton-block"></div>
		<div class="skeleton skeleton-block"></div>
		<div class="skeleton skeleton-block"></div>
		<div class="skeleton skeleton-button-block"></div>
	{:else}
		<div class="access block">
			<div class="content">
				<p>Prieiga</p>
				<p>Pirmoji prieiga: <span>{formatDate(policyDetails?.firstAccessAt ?? '—')}</span></p>
				<p>Prieigų skaičius: <span>{policyDetails?.accessCount ?? '—'}</span></p>
				<p>Paskutinė prieiga: <span>{formatDate(policyDetails?.lastAccessAt ?? '—')}</span></p>
			</div>
		</div>
		<div class="release-details block">
			<div class="content">
				<p>Atskleidimo informacija</p>
				<p>Atskleidė: {policyDetails?.releasedBy ?? '—'}</p>
				<p>Failų skaičius: {policyDetails?.policyFiles.length ?? '—'} files</p>
				<p>Prieinama nuo: {formatDate(policyDetails?.releasedAt ?? '—')}</p>
			</div>
		</div>
		<div class="files block">
			<div class="block-header">
				<File size={16} strokeWidth={1.5} />
				<h3>Failai</h3>
			</div>
			<hr />
			<div class="content">
				{#if !policyDetails?.policyFiles?.length}
					<p class="empty-files-message">
						Savininkas galimai pašalino šiai politikai priskirtus failus...
					</p>
				{:else}
					{#each policyDetails?.policyFiles ?? [] as file (file.id)}
						<div class="file">
							{#if file.loading}
								<div class="skeleton-line"></div>
							{:else}
								<img src="/img/file-icon.png" alt="file-icon" width="18" />
								<div class="file-details">
									{#if file.fileName}
										<p>Failo pavadinimas: {shortFileName(file.fileName)}</p>
									{/if}
									{#if file.fileSize != null}
										<p>Dydis: {convertFileSize(file.fileSize)}</p>
									{/if}
									<p>Sukurta: {formatDate(file.createdAt)}</p>
								</div>
								<div class="file-actions">
									{#if file.fileName}
										<button
											onclick={(e) => {
												e.stopPropagation();
												onHideOne(file.id);
											}}><Eye size={16} strokeWidth={1.5} /></button
										>
									{:else}
										<button
											onclick={(e) => {
												e.stopPropagation();
												onDecryptOne(file.id);
											}}><EyeOff size={16} strokeWidth={1.5} /></button
										>
									{/if}

									<button
										onclick={(e) => {
											e.stopPropagation();
											onDownload(file.id);
										}}
									>
										<Download size={16} />
									</button>
								</div>
							{/if}
						</div>
					{/each}
				{/if}
			</div>
		</div>
		<div class="button block">
			<div class="content">
				{#if (policyDetails?.policyFiles?.length ?? 0) > 1}
					<button class="action-btn" onclick={() => onDecryptAll()}
						><LockOpen size={16} />Iššifruoti visus</button
					>
					<div class="button-divider"></div>
					<button class="action-btn" onclick={() => onHideAll()}
						><Download size={16} />Slėpti visus</button
					>

					<button class="action-btn" onclick={() => onDownloadAll()}
						><Download size={16} />Atsisiųsti visus</button
					>
				{/if}
				{#if (policyDetails?.policyFiles?.length ?? 0) > 1}
					<div class="button-divider"></div>
				{/if}
				{#if policyDetails?.policyId}
					<button class="action-btn delete" onclick={() => onDelete(policyDetails.policyId)}
						><Trash2 size={16} />Ištrinti politiką</button
					>
				{/if}
			</div>
		</div>
	{/if}
</div>

<style>
	h3 {
		font-weight: 500;
	}

	hr {
		opacity: 0.2;
		margin: 5px 0;
	}

	.block {
		background-color: #fff;
		border-radius: 7px;
		padding: 15px;
		margin-top: 15px;
	}

	.block-header {
		display: inline-flex;
		align-items: center;
		gap: 0.8rem;
		font-weight: 400;
	}

	.header-upper {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		margin-bottom: 0.5rem;
		column-gap: 0.5rem;
		row-gap: 0.4rem;
	}

	.header-upper h1 {
		line-height: 1;
	}

	.pill {
		font-size: 0.85rem;
	}

	.pills .pill:last-child {
		margin-left: 0.2rem;
	}

	.pill.releaseType {
		background-color: #d3d3d3;
	}

	:is(.release-details.block, .access.block) .content p:first-child {
		margin-bottom: 0.5rem;
		font-weight: 600;
	}

	:is(.release-details.block, .access.block) .content p:not(:first-child) {
		font-size: 0.8rem;
		margin-bottom: 0.3rem;
	}

	.files.block .content {
		max-height: 500px;
		overflow-y: auto;
		padding-right: 0.4rem;
		scrollbar-gutter: stable; /* prevents layout shift when scrollbar appears */
		margin-top: 0.5rem;
	}

	.files.block .content .file {
		display: flex;
		align-items: center;
		gap: 1rem;
		font-size: 0.85rem;
	}

	.files.block .content button {
		display: flex;
		margin-left: auto;
		cursor: pointer;
		background-color: transparent;
		border: none;
	}

	.file:not(:first-child) {
		margin-top: 1rem;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar {
		width: 0.4rem;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar-button {
		width: 0;
		height: 0;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar-track {
		background: transparent;
	}

	:is(.recipients.block .content, .files.block .content)::-webkit-scrollbar-thumb {
		background: rgba(120, 120, 120, 0.55);
		border-radius: 999px;
	}

	.button.block {
		margin-top: 0.5rem;
	}

	.button.block .content {
		display: flex;
		justify-content: space-between;
		flex-wrap: wrap;
		gap: 0.75rem;
	}

	.action-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		background-color: #fff;
		border-radius: 0.5rem;
		padding: 0.4rem 1rem;
		font-size: 0.85rem;
		font-weight: 500;
		border: 1px solid rgba(83, 83, 83, 0.3);
		gap: 0.6rem;
		box-shadow: 1px 2px 4px rgba(0, 0, 0, 0.1);
		transition: transform 180ms ease;
		width: 100%;
	}

	.action-btn:active {
		transform: translateY(0) scale(0.98);
	}

	.action-btn.delete {
		background: #fff5f5;
		border: 1px solid #f1b5b5;
		color: #b42318;
	}

	.button-divider {
		width: 1px;
		align-self: stretch;
		background: #e5e7eb;
	}

	.empty-files-message {
		padding: 0.5rem;
		font-size: 0.8rem;
		font-style: italic;
	}

	.skeleton {
		display: inline-block;
		border-radius: 999px;
		width: 100%;
		position: relative;
		overflow: hidden;
		background: rgba(0, 0, 0, 0.1);
	}

	.skeleton::after {
		content: '';
		position: absolute;
		inset: 0;
		transform: translateX(-100%);
		background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
		animation: skeleton-loading 1.1s infinite;
	}

	.skeleton-title {
		width: 250px;
		height: 30px;
	}

	.skeleton-pill {
		width: 90px;
		height: 25px;
		border-radius: 999px;
	}

	.skeleton-block {
		margin-top: 15px;
		width: 100%;
		height: 130px;
		border-radius: 16px;
	}

	.file-actions {
		margin-left: auto;
		display: flex;
		gap: 0.5rem;
	}

	.skeleton-line {
		width: 100%;
		display: inline-block;
		height: 0.7rem;
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
		animation: skeleton-loading 1.1s infinite;
	}

	@keyframes skeleton-loading {
		100% {
			transform: translateX(100%);
		}
	}
</style>
