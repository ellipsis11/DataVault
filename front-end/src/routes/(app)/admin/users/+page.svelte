<script lang="ts">
	import {
		toast,
		getAdminUsersPage,
		type AdminUserListItem,
		type AdminUserDetails,
		getAdminUserDetails,
		deleteUser,
		getAdminAuditLogDetails,
		type AdminAuditLogListItemDetails
	} from '$lib';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import TablePagination from '$lib/components/TablePagination.svelte';
	import UserDetailsSide from '$lib/components/UserDetailsSide.svelte';
	import UserTable from '$lib/components/UserTable.svelte';
	import { Search } from 'lucide-svelte';
	import { onMount } from 'svelte';

	let { data } = $props();
	let user = $derived(data.user);
	let userListItems = $state<AdminUserListItem[]>([]);
	let userDetails = $state<AdminUserDetails | null>(null);
	let initLoading = $state<boolean>(false);
	let error = $state<string | null>(null);
	let page = $state<number>(0);
	let size = $state<10 | 20 | 50 | 100>(10);
	let total = $state<number>(0);
	let sideOpen = $state<boolean>(false);
	let initLoadingDetails = $state<boolean>(false);
	let deleteDialogOpen = $state<boolean>(false);
	let userToDelete = $state<string | null>(null);
	let adminAuditLogDetails = $state<AdminAuditLogListItemDetails | null>(null);
	let searchTerm = $state<string>('');

	const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

	onMount(() => {
		loadUsersPage();

		const onClick = (e: MouseEvent) => {
			const root = (e.target as HTMLElement).closest('.side-panel, .form, .modal');
			if (!root) sideOpen = false;
		};

		document.addEventListener('click', onClick);

		return () => {
			document.removeEventListener('click', onClick);
		};
	});

	async function loadUsersPage(refreshPage = false) {
		initLoading = true;
		if (refreshPage) page = 0;

		try {
			const userPage = await getAdminUsersPage({ query: searchTerm.trim(), page, size });
			userListItems = userPage.userList;
			total = userPage.totalElements;
		} catch (e) {
			error = e instanceof Error ? e.message : 'Nepavyko gauti naudotojų puslapio!';
			toast.error(error);
		} finally {
			await delay(500);
			initLoading = false;
		}
	}

	async function onSelectUser(userId: string) {
		initLoadingDetails = sideOpen = true;
		try {
			userDetails = await getAdminUserDetails(userId);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko gauti naudotojo informacijos!');
		} finally {
			await delay(500);
			initLoadingDetails = false;
		}
	}

	function onDeleteUser(userId: string) {
		deleteDialogOpen = true;
		userToDelete = userId;
	}

	async function onConfirmDeleteUser() {
		if (!userToDelete) {
			toast.error('Nepasirinktas naudotojas ištrynimui!');
			return;
		}
		try {
			await deleteUser(userToDelete);
			sideOpen = false;
			toast.success('Naudotojas sėkmingai ištrintas');
			loadUsersPage();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko ištrinti naudotojo!');
		} finally {
			deleteDialogOpen = false;
		}
	}

	function onClose() {
		deleteDialogOpen = false;
		userToDelete = null;
	}

	async function onSelectedLog(logId: string) {
		try {
			adminAuditLogDetails = await getAdminAuditLogDetails(logId);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko gauti audito registro irašų!');
		}
	}

	function onSizeChange(newSize: 10 | 20 | 50 | 100) {
		size = newSize;
		page = 0;
		loadUsersPage();
	}

	function onPageChange(newPage: number) {
		page = newPage;
		loadUsersPage();
	}

	function onCloseUserDetailsDialog() {
		sideOpen = false;
	}
</script>

<svelte:head>
	<title>Naudotojų valdymas</title>
</svelte:head>

<div class="user-management">
	<div class="header-wrapper">
		<div class="header">
			<h2>Naudotojų valdymas</h2>
			<p>Peržiūrėkite registruotus naudotojus, jų paskyros informaciją ir veiklos istoriją.</p>
		</div>
	</div>
	<div class="body">
		<div class="body-top">
			<div class="search-sorting">
				<div class="search">
					<button type="button" aria-label="Search"><Search size={20} /></button>
					<input
						type="text"
						placeholder="Ieškoti pagal el. paštą, ID..."
						bind:value={searchTerm}
						oninput={() => loadUsersPage(true)}
					/>
				</div>
			</div>
			<div class="total-users-count">
				<p>Iš viso naudotojų: {userListItems.length}</p>
			</div>
		</div>
		<UserTable {userListItems} {initLoading} {error} {searchTerm} {onSelectUser} />
		<TablePagination {page} {size} {total} {onSizeChange} {onPageChange} />
	</div>
	<div class="side-panel" class:open={sideOpen}>
		<UserDetailsSide
			{userDetails}
			initLoading={initLoadingDetails}
			{onDeleteUser}
			{user}
			{onSelectedLog}
			{adminAuditLogDetails}
			{onCloseUserDetailsDialog}
		/>
	</div>
	{#if deleteDialogOpen}
		<ConfirmDialog
			title="Ištrinti naudotoją"
			message="Šio veiksmo nebus galima atšaukti. Ar tikrai norite tęsti?"
			cancelLabel="Atšaukti"
			confirmLabel="Ištrinti"
			{onClose}
			onConfirm={onConfirmDeleteUser}
		/>
	{/if}
</div>

<style>
	.user-management {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}
	.body {
		background-color: #fff;
		padding: 1rem;
	}

	.body-top {
		display: flex;
		align-items: center;
	}

	.header-wrapper {
		display: flex;
		justify-content: space-between;
		background-color: #fff;
		padding: 10px 20px;
		border-bottom: 1px solid var(--border-cl);
	}

	.header p {
		margin-top: 0.2rem;
		font-size: 0.8rem;
		opacity: 0.7;
	}

	.search-sorting {
		max-width: 400px;
		width: 100%;
	}

	.search {
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

	.side-panel {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		background-color: #fff;
		padding: 1rem 0.5rem 1rem 1.2rem;
		width: 500px;
		max-width: 100vw;
		overflow-y: auto;
		overflow-x: hidden;
		min-height: 0;
	}

	.total-users-count {
		font-size: 0.8rem;
		font-weight: 600;
		margin-left: auto;
		padding-left: 0.5rem;
	}

	*::-webkit-scrollbar-button {
		display: none;
		width: 0;
		height: 0;
	}

	*::-webkit-scrollbar {
		width: 6px;
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

	@media (max-width: 1200px) {
	}

	@media (max-width: 768px) {
	}

	@media (max-width: 600px) {
		.user-management {
			margin-top: 1rem;
		}
	}
</style>
