<script lang="ts">
	import {
		Clock,
		User,
		Shield,
		NotebookText,
		RectangleEllipsis,
		Braces,
		ArrowLeft
	} from 'lucide-svelte';
	import { formatDate, type AdminAuditLogListItemDetails } from '$lib';
	import JsonPreview from '$lib/components/JsonPreview.svelte';
	let { auditLogDetails, initLoading, onCloseAuditLogsDetailsDialog } = $props<{
		auditLogDetails: AdminAuditLogListItemDetails | null;
		initLoading: boolean;
		onCloseAuditLogsDetailsDialog: () => void;
	}>();
</script>

<div class="header">
	<div class="go-back-btn">
		<button onclick={onCloseAuditLogsDetailsDialog}
			><ArrowLeft size={18} strokeWidth={1.5} />Atgal</button
		>
	</div>
	{#if initLoading}
		<div class="header-top">
			<div class="skeleton-line skeleton-title"></div>
			<div class="skeleton-line skeleton-pill"></div>
		</div>
		<div class="skeleton-line skeleton-p"></div>
	{:else}
		<div class="header-top">
			<h1>Įrašo informacija</h1>
			<span class="pill" data-status={auditLogDetails?.level ?? '–'}
				>{auditLogDetails?.level ?? '—'}</span
			>
		</div>
		<p class="action-type">{auditLogDetails?.actionType ?? '–'}</p>
	{/if}
</div>
<div class="body">
	{#if initLoading}
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
		<div class="skeleton-line skeleton-block"></div>
	{:else}
		<div class="log-details block">
			<div class="item">
				<div class="item-name">
					<Clock />
					<span>Laiko žyma</span>
				</div>
				<span class="item-value">{formatDate(auditLogDetails?.createdAt ?? '–')}</span>
			</div>
			<div class="item">
				<div class="item-name">
					<User />
					<span>Naudotojas</span>
				</div>
				<span class="item-value"
					>{auditLogDetails?.actorEmail ?? auditLogDetails?.userId ?? '–'}</span
				>
			</div>
			<div class="item">
				<div class="item-name">
					<Shield />
					<span>Lygis</span>
				</div>
				<span class="item-value">{auditLogDetails?.level ?? '–'}</span>
			</div>
			<div class="item">
				<div class="item-name">
					<NotebookText />
					<span>Veiksmas</span>
				</div>
				<span class="item-value">{auditLogDetails?.actionType ?? '–'}</span>
			</div>
			<div class="item">
				<div class="item-name">
					<RectangleEllipsis />
					<span>Aprašymas</span>
				</div>
				<span class="item-value">{auditLogDetails?.message ?? '–'}</span>
			</div>
			<div class="item metadata">
				<div class="item-name">
					<Braces />
					<span>Metaduomenys</span>
				</div>
				<div class="metabloc"><JsonPreview json={auditLogDetails?.metaData} /></div>
			</div>
		</div>
	{/if}
</div>

<style>
	.block {
		background-color: #fff;
		border-radius: 7px;
	}

	.item {
		display: grid;
		grid-template-columns: 1fr 1fr;
		border-top: 2px solid #f3f3f3;
		padding: 1.5rem 0;
		font-size: 0.9rem;
		font-weight: 600;
		color: #404048;
	}

	.item-name {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.item-value {
		justify-self: end;
		width: min(100%, 360px);
		min-width: 0;
		white-space: normal;
		overflow-wrap: break-word;
		line-height: 1.5;
	}

	.header {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.header-top {
		display: flex;
		gap: 1rem;
		align-items: center;
	}

	.header .action-type {
		font-size: 0.85rem;
	}

	.pill {
		font-size: 0.85rem;
		border-radius: 7px;
	}

	.pill[data-status='ALERT'] {
		background: #fee2e2;
		color: #b91c1c;
	}

	.pill[data-status='INFO'] {
		background: #dbeafe;
		color: #1d4ed8;
	}

	.item.metadata {
		display: block;
	}

	.metabloc {
		margin-top: 1rem;
	}

	.skeleton-title {
		width: 15.625rem;
		height: 1.4rem;
	}

	.skeleton-p {
		margin-top: 0.3rem;
		width: 5rem;
		height: 0.8rem;
	}

	.skeleton-pill {
		display: inline-block;
		width: 3.2rem;
		height: 1.5rem;
		border-radius: 5px;
	}

	.skeleton-block {
		width: 100%;
		height: 5rem;
		border-radius: 1rem;
	}

	.skeleton-block:not(:first-child) {
		margin-top: 0.9375rem;
	}
</style>
