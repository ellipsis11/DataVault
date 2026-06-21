<script lang="ts">
	import { formatDateParts, highlightText, type AdminUserListItem } from '$lib';
	import UserAvatar from './UserAvatar.svelte';

	type Props = {
		userListItems: AdminUserListItem[];
		initLoading: boolean;
		error: string | null;
		searchTerm: string;
		onSelectUser: (userId: string) => void;
	};

	let { userListItems, initLoading, error, searchTerm, onSelectUser }: Props = $props();
</script>

<div class="table-wrap">
	<table>
		<thead>
			<tr>
				<th>Naudotojas</th>
				<th>Rolė</th>
				<th>Sukurta</th>
				<th>Paskutinis prisijungimas</th>
				<th>Audito įrašai</th>
			</tr>
		</thead>
		<tbody class:blurred={initLoading}>
			{#if error}
				<tr>
					<td colspan="5" class="error">{error}</td>
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
			{:else if userListItems.length === 0}
				<tr>
					<td colspan="5" class="empty">Naudotojų nerasta.</td>
				</tr>
			{:else}
				{#each userListItems as user (user.id)}
					{@const createdAt = formatDateParts(user.createdAt)}
					{@const lastLoginAt = formatDateParts(user.lastLoginAt)}
					<tr
						class="table-row"
						onclick={(e) => {
							e.stopPropagation();
							onSelectUser(user.id);
						}}
					>
						<td data-label="Naudotojas">
							<div class="user-avatar">
								<UserAvatar email={user.email} />
							</div>
							<div class="user-id">
								{#if user.email}
									<span class="email">{@html highlightText(user.email, searchTerm)}</span>
								{/if}
								<span class="id">{@html highlightText(user.id, searchTerm)}</span>
							</div>
						</td>
						<td data-label="Rolė"
							><span class="role-badge" data-status={user.role}>{user.role}</span></td
						>
						<td data-label="Sukurta"
							><div class="dates">
								<span class="date">{createdAt.date}</span>
								<span class="time">{createdAt.time}</span>
							</div>
						</td>
						<td data-label="Paskutinis prisijungimas"
							><div class="dates">
								<span class="date">{lastLoginAt.date}</span>
								<span class="time">{lastLoginAt.time}</span>
							</div></td
						>
						<td data-label="Audito įrašai" class="log-count">{user.logCount}</td>
					</tr>
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
		margin-top: 1rem;
	}

	table {
		width: 100%;
		border: 1px solid #00000012;
		border-radius: 7px;
		background: rgba(255, 255, 255, 0.7);
		border-spacing: 0;
		table-layout: auto; /* longest content controls column width */
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

	.table-row {
		cursor: pointer;
	}

	.table-row td:first-child {
		display: flex;
		align-items: center;
		gap: 1rem;
	}

	.user-id {
		display: flex;
		flex-direction: column;
	}

	.user-id:has(.email) {
		gap: 0.3rem;
	}

	.user-id .email,
	.dates .date,
	.log-count {
		font-size: 0.9rem;
		font-weight: 700;
	}

	.user-id .id,
	.dates .time {
		font-size: 0.8rem;
		font-weight: 700;
		color: #8c90a5;
	}

	.dates {
		display: flex;
		flex-direction: column;
	}

	.skeleton-row td {
		padding-top: 12px;
		padding-bottom: 12px;
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
</style>
