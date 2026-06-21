<script lang="ts">
	import { page as Page } from '$app/state';
	import {
		toast,
		exportAllAuditLogsForAdmin,
		getAdminAuditLogPage,
		getAdminAuditLogDetails,
		getAuditLogsByProvideDates,
		type AdminAuditLogListItem,
		type AdminAuditLogListItemDetails,
		type AuditActionFilter
	} from '$lib';
	import AuditLogsDetailsSide from '$lib/components/AuditLogsDetailsSide.svelte';
	import Calendar from '$lib/components/Calendar.svelte';
	import LogTable from '$lib/components/LogTable.svelte';
	import TablePagination from '$lib/components/TablePagination.svelte';
	import { Search, Download } from 'lucide-svelte';
	import { onMount } from 'svelte';

	let auditLogListItems = $state<AdminAuditLogListItem[]>([]);
	let auditLogDetails = $state<AdminAuditLogListItemDetails | null>(null);
	let initLoading = $state<boolean>(false);
	let error = $state<string | null>(null);
	let page = $state<number>(0);
	let size = $state<10 | 20 | 50 | 100>(10);
	let total = $state<number>(0);
	let sideOpen = $state<boolean>(false);
	let initLoadingDetails = $state<boolean>(false);
	let selectedUserId = $state<string | undefined>(undefined);
	let searchTerm = $state<string>('');
	let filter = $state<AuditActionFilter | null>(null);

	onMount(() => {
		const onClick = (e: MouseEvent) => {
			const root = (e.target as HTMLElement).closest('.side-panel, .form');
			if (!root) sideOpen = false;
		};

		document.addEventListener('click', onClick);
		loadAuditLogPage();

		return () => {
			document.removeEventListener('click', onClick);
		};
	});

	async function loadAuditLogPage(refreshPage = false) {
		selectedUserId = Page.url.searchParams.get('userId') ?? undefined;
		initLoading = true;
		if (refreshPage) page = 0;

		try {
			const auditLogPage = await getAdminAuditLogPage({
				userId: selectedUserId,
				query: searchTerm.trim(),
				filter: filter,
				page: page,
				size: size
			});

			auditLogListItems = auditLogPage.logList;
			total = auditLogPage.totalElements;

			if (!auditLogPage.logChainIntegrityValid) {
				toast.error('Aptiktas audito registrų vientisumo pažeidimas!');
			}
		} catch (e) {
			error = e instanceof Error ? e.message : 'Nepavyko gauti audito registro puslapio!';
			toast.error(error);
		} finally {
			await delay(500);
			initLoading = false;
		}
	}

	async function onExportAuditLogs() {
		try {
			await exportAllAuditLogsForAdmin();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko eksportuoti audito registro įrašų!');
		}
	}

	async function onSelectLog(logId: string) {
		initLoadingDetails = sideOpen = true;
		try {
			auditLogDetails = await getAdminAuditLogDetails(logId);
		} catch (e) {
			toast.error(
				e instanceof Error ? e.message : 'Nepavyko gauti audito registro įrašo informacijos!'
			);
		} finally {
			setTimeout(() => (initLoadingDetails = false), 1000);
		}
	}

	async function onDatePicked(fromDate: string, toDate: string) {
		try {
			const auditLogPage = await getAuditLogsByProvideDates(fromDate, toDate, page, size);
			auditLogListItems = auditLogPage.logList;
			total = auditLogPage.totalElements;
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko gauti audito registro puslapio!');
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

	function onCloseAuditLogsDetailsDialog() {
		sideOpen = false;
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
		<div class="body-top">
			<div class="search-sorting">
				<div class="search">
					<button type="button" aria-label="Search"><Search size={20} /></button>
					<input
						type="text"
						placeholder="Ieškoti pagal el. paštą, ID arba aprašymą..."
						bind:value={searchTerm}
						oninput={() => loadAuditLogPage(true)}
					/>
				</div>
				<div class="table-sorting-btns">
					<button class:active={filter === null} onclick={() => onFilterClick(null)}>Visi</button>
					<button class:active={filter === 'ACCOUNT'} onclick={() => onFilterClick('ACCOUNT')}
						>Paskyra</button
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
				<div class="calendar">
					<Calendar {onDatePicked} />
				</div>
			</div>
		</div>
		<LogTable
			{auditLogListItems}
			{initLoading}
			{error}
			{searchTerm}
			purpose="admin"
			{onSelectLog}
		/>
		<TablePagination {page} {size} {total} {onSizeChange} {onPageChange} />
	</div>
	<div class="side-panel" class:open={sideOpen}>
		<AuditLogsDetailsSide
			{auditLogDetails}
			initLoading={initLoadingDetails}
			{onCloseAuditLogsDetailsDialog}
		/>
	</div>
</div>

<style>
	.audit-logs {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	.body {
		background-color: #fff;
		padding: 1rem;
	}

	/* .body-top {
		display: flex;
		align-items: center;
	} */

	.calendar {
		margin-left: auto;
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

	.search {
		max-width: 400px;
		display: flex;
		align-items: center;
		gap: 0.8rem;
		padding: 0.5rem;
		border: 1px solid rgb(216, 216, 216);
		border-radius: 7px;
		background-color: #fff;
	}

	.search button {
		background-color: transparent;
		border: none;
	}

	.search input {
		font-size: 0.8rem;
		border: none;
		outline: none;
		width: 100%;
		margin-bottom: 2px;
	}

	.table-sorting-btns {
		margin-top: 1rem;
	}

	.table-sorting-btns button:not(:last-child) {
		margin-right: 4px;
	}

	.side-panel {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		background-color: #fff;
		padding: 1rem 1.2rem;
	}

	.search-sorting {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-template-rows: 40px auto;
		column-gap: 0.5rem;
	}

	.table-sorting-btns {
		grid-column: 1 / -1;
		grid-row: 2;
	}

	@media (max-width: 1200px) {
	}

	@media (max-width: 800px) {
		.search-sorting {
			grid-template-columns: 1fr;
			gap: 0.7rem;
		}

		.calendar {
			grid-row: 2;
			margin-left: 0;
		}

		.table-sorting-btns {
			grid-column: auto;
			grid-row: auto;
		}

		.table-sorting-btns {
			margin-top: 0;
		}
	}

	@media (max-width: 600px) {
		.audit-logs {
			margin-top: 1rem;
		}
	}
</style>
