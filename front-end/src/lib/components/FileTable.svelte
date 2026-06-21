<script lang="ts">
	import { convertFileSize, formatDate, storageConverter, type FileRow } from '$lib';
	import {
		File as FileIcon,
		Menu,
		Ellipsis,
		Download,
		FolderPen,
		Trash,
		Lock,
		LockOpen
	} from 'lucide-svelte';
	import { onMount } from 'svelte';

	let { fileRows, initLoading, error, onDecryptOne, onHideOne, onDownload, onRename, onDelete } =
		$props<{
			fileRows: FileRow[];
			initLoading: boolean;
			error: string | null;
			onDecryptOne: (fileId: string) => void;
			onHideOne: (fileId: string) => void;
			onDownload: (filedId: string) => void;
			onRename: (fileId: string) => void;
			onDelete: (field: string) => void;
		}>();
	let openMenuId = $state<string | null>(null);

	onMount(() => {
		const onClick = (e: MouseEvent) => {
			const root = (e.target as HTMLElement).closest('.dropdown-anchor');
			if (!root) openMenuId = null;
		};
		document.addEventListener('click', onClick);
		return () => document.removeEventListener('click', onClick);
	});

	function toggleMenu(fileRowId: string) {
		openMenuId = fileRowId === openMenuId ? null : fileRowId;
	}
</script>

{#snippet fileActions(fileRow: FileRow)}
	<div class="dropdown-anchor">
		<button class="meatballs-btn" onclick={() => toggleMenu(fileRow.id)}><Ellipsis /></button>
		{#if openMenuId === fileRow.id}
			<div class="dropdown-button-menu">
				<ul>
					<li>
						{#if !fileRow.fileMeta}
							<button
								onclick={() => {
									onDecryptOne(fileRow.id);
									openMenuId = null;
								}}><LockOpen size={16} /> Rodyti meta</button
							>
						{:else}
							<button
								onclick={() => {
									onHideOne(fileRow.id);
									openMenuId = null;
								}}><Lock size={16} /> Slėpti meta</button
							>
						{/if}
					</li>
					<li>
						<button
							onclick={() => {
								onDownload(fileRow.id);
								openMenuId = null;
							}}><Download size={16} /> Atsisiųsti</button
						>
					</li>
					<li>
						<button
							onclick={() => {
								onRename(fileRow.id);
								openMenuId = null;
							}}><FolderPen size={16} /> Pervadinti</button
						>
					</li>
					<li>
						<button
							onclick={() => {
								onDelete(fileRow.id);
								openMenuId = null;
							}}><Trash size={16} /> Ištrinti</button
						>
					</li>
				</ul>
			</div>
		{/if}
	</div>
{/snippet}

<div class="table-wrap">
	<table>
		<thead>
			<tr>
				<th>Pavadinimas</th>
				<th>Tipas</th>
				<th>Dydis</th>
				<th>Saugykla</th>
				<th>Sukurta</th>
			</tr>
		</thead>
		<tbody class:blurred={initLoading}>
			{#if error}
				<tr>
					<td colspan="7" class="error">{error}</td>
				</tr>
			{:else if initLoading}
				{#each Array(8) as _}
					<tr class="skeleton-row">
						{#each Array(5) as _}
							<td><span class="skeleton-line"></span></td>
							<td class="mobile">
								<span class="skeleton-line"></span>
								<span class="skeleton-line"></span>
							</td>
						{/each}
					</tr>
				{/each}
			{:else if fileRows.length === 0}
				<tr>
					<td colspan="7" class="empty">Failų nerasta.</td>
				</tr>
			{:else}
				{#each fileRows as fileRow (fileRow.id)}
					{#if fileRow.loading === true}
						<tr class="skeleton-row">
							{#each Array(5) as _}
								<td><span class="skeleton-line"></span></td>
								<td class="mobile">
									<span class="skeleton-line"></span>
									<span class="skeleton-line"></span>
								</td>
							{/each}
						</tr>
					{:else}
						<tr>
							<td class="name">
								<span class="file-icon"><FileIcon size={20} /></span>
								<span class="file-name" class:encrypted={!fileRow.fileMeta}
									>{fileRow.fileMeta?.fileName ?? 'xxxxxxxxxx'}</span
								>
								{@render fileActions(fileRow)}
							</td>
							<td data-label="Tipas" class="type" class:encrypted={!fileRow.fileMeta}
								>{fileRow.fileMeta?.mimeType ?? 'xxxxxxxxxx'}</td
							>
							<td data-label="Dydis" class="size" class:encrypted={!fileRow.fileMeta}
								>{fileRow.fileMeta?.size != null
									? convertFileSize(fileRow.fileMeta.size)
									: 'xxxxxxxxxx'}</td
							>
							<td data-label="Saugykla" class="storage"
								>{storageConverter(fileRow.storageType) ?? 'xxxxxxxxxx'}</td
							>
							<td data-label="Sukurta" class="created"
								><span>{formatDate(fileRow.createdAt) ?? 'xxxxxxxxxx'}</span>
								{@render fileActions(fileRow)}
							</td>
						</tr>
					{/if}
				{/each}
			{/if}
		</tbody>
	</table>
	{#if initLoading}
		<div class="overlay">
			<span class="spinner"></span>
		</div>
	{/if}
</div>

<style>
	.table-wrap {
		position: relative;
	}

	table {
		width: 100%;
		border: 1px solid #00000012;
		border-radius: 7px;
		background: rgba(255, 255, 255, 0.7);
		border-spacing: 0;
		table-layout: auto;
	}

	thead th {
		font-weight: 700;
		color: rgba(0, 0, 0, 0.6);
		background: rgba(0, 0, 0, 0.03);
		border-bottom: 1px solid #00000012;
		padding: 10px 8px;
		text-align: left;
	}

	tbody td {
		padding: 10px 8px;
		border-top: 1px solid #00000012;
	}

	table td,
	table th {
		vertical-align: middle;
	}

	td.name {
		display: flex;
		align-items: center;
		gap: 0.2rem;
	}

	.file-icon {
		display: flex;
	}

	.file-name {
		overflow-wrap: anywhere;
	}

	tbody td.created {
		display: flex;
		justify-content: space-between;
	}

	.meatballs-btn {
		display: flex;
		align-items: center;
		margin-right: 0.3rem;
		background-color: transparent;
		border: none;
		font-size: 1.3rem;
		line-height: 1;
		border-radius: 7px;
	}

	.meatballs-btn:hover {
		background-color: #e0e0e0;
	}

	.error {
		text-align: center;
		color: #c00;
		padding: 14px;
	}

	.empty {
		font-style: italic;
		padding: 0.8rem;
		opacity: 0.8;
		text-align: center;
	}

	.encrypted {
		letter-spacing: 1px;
		color: #9aa0a6;
		font-style: italic;
		opacity: 0.8;
	}

	.name .dropdown-anchor {
		display: none;
	}

	@media (max-width: 1000px) {
		.created .dropdown-anchor {
			display: none;
		}

		.name .dropdown-anchor {
			display: block;
			margin-left: auto;
		}

		.created {
			min-width: 0;
		}

		.created span {
			overflow-wrap: anywhere;
		}
	}
</style>
