<script lang="ts">
	import {
		toast,
		exportAuditLogs,
		getAuditLogPage,
		type AuditLogListItem,
		type AuditActionFilter
	} from '$lib';
	import LogTable from '$lib/components/LogTable.svelte';
	import TablePagination from '$lib/components/TablePagination.svelte';
	import { Search, Download } from 'lucide-svelte';
	import { onMount } from 'svelte';

	let auditLogListItems = $state<AuditLogListItem[]>([]);
	let initLoading = $state<boolean>(false);
	let error = $state<string | null>(null);
	let page = $state<number>(0);
	let size = $state<10 | 20 | 50 | 100>(10);
	let total = $state<number>(0);
	let searchTerm = $state<string>('');
	let filter = $state<AuditActionFilter | null>(null);

	async function loadAuditLogPage(refreshPage = false) {
		initLoading = true;
		if (refreshPage) page = 0;
		try {
			const auditLogPage = await getAuditLogPage({
				query: searchTerm.trim(),
				filter,
				page,
				size
			});

			auditLogListItems = auditLogPage.logList;
			total = auditLogPage.totalElements;
		} catch (e) {
			error = e instanceof Error ? e.message : '!Įvyko klaida!';
			toast.error(error);
		} finally {
			await delay(500);
			initLoading = false;
		}
	}

	onMount(() => {
		(async () => {
			await loadAuditLogPage();
		})();
	});

	async function onExportAuditLogs() {
		try {
			await exportAuditLogs();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko eksportuoti audito žurnalų!');
		}
	}

	const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

	function onSizeChange(newSize: 10 | 20 | 50 | 100) {
		size = newSize;
		page = 0;
		loadAuditLogPage();
	}

	function onPageChange(newPage: number) {
		page = newPage;
		loadAuditLogPage();
	}

	async function onFilterClick(selectedFilter: AuditActionFilter | null) {
		filter = selectedFilter;
		await loadAuditLogPage(true);
	}
</script>

<svelte:head>
	<title>Audito registrai</title>
</svelte:head>

<div class="audit-logs">
	<div class="header-wrapper">
		<div class="header">
			<h2>Audito registrai</h2>
			<p>Peržiūrėkite naujausius sistemoje užfiksuotus veiksmus.</p>
		</div>
		<button onclick={onExportAuditLogs}><Download size={20} />Eksportuoti viską</button>
	</div>
	<div class="body">
		<div class="search-sorting">
			<div class="search">
				<button type="button" aria-label="Search"><Search size={20} /></button>
				<input
					type="text"
					placeholder="Ieškoti pagal aprašymą..."
					bind:value={searchTerm}
					oninput={() => loadAuditLogPage(true)}
				/>
			</div>
			<div class="table-sorting-btns">
				<button class:active={filter === null} onclick={() => onFilterClick(null)}>Visi</button>
				<button class:active={filter === 'ACCOUNT'} onclick={() => onFilterClick('ACCOUNT')}>
					Paskyra</button
				>
				<button class:active={filter === 'FILES'} onclick={() => onFilterClick('FILES')}
					>Failai</button
				>
				<button class:active={filter === 'POLICIES'} onclick={() => onFilterClick('POLICIES')}
					>Politikos</button
				>
				<button class:active={filter === 'TELEGRAM'} onclick={() => onFilterClick('TELEGRAM')}
					>Telegram</button
				>
			</div>
		</div>
		<LogTable {auditLogListItems} {initLoading} {error} {searchTerm} purpose="user" />
		<TablePagination {page} {size} {total} {onSizeChange} {onPageChange} />
	</div>
</div>

<style>
	.audit-logs {
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
		.audit-logs {
			margin-top: 1rem;
		}
	}
</style>
