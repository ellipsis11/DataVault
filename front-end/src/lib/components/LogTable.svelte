<script lang="ts">
	import {
		formatDate,
		translateLogLevel,
		translateLogAction,
		highlightText,
		type AuditLogListItem,
		type AdminAuditLogListItem,
		type LogTablePurpose
	} from '$lib';

	type Props = {
		auditLogListItems: AuditLogListItem[] | AdminAuditLogListItem[];
		initLoading: boolean;
		error: string | null;
		searchTerm: string;
		purpose: LogTablePurpose;
		onSelectLog?: (logId: string) => void;
	};

	let { auditLogListItems, initLoading, error, searchTerm, purpose, onSelectLog }: Props = $props();
</script>

<div class="table-wrap">
	<table>
		<thead>
			<tr>
				<th>Laiko žyma</th>
				{#if purpose === 'admin'}
					<th>El. paštas / Naudotojo ID</th>
				{/if}
				<th>Lygis</th>
				<th>Veiksmas</th>
				<th>Aprašymas</th>
				<th>Vientisumas</th>
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
						{#if purpose === 'admin'}
							{#each Array(6) as _}
								<td><span class="skeleton-line"></span></td>
								<td class="mobile">
									<span class="skeleton-line"></span>
									<span class="skeleton-line"></span>
								</td>
							{/each}
						{:else}
							{#each Array(5) as _}
								<td><span class="skeleton-line"></span></td>
								<td class="mobile">
									<span class="skeleton-line"></span>
									<span class="skeleton-line"></span>
								</td>
							{/each}
						{/if}
					</tr>
				{/each}
			{:else if auditLogListItems.length === 0}
				<tr>
					<td colspan="5" class="empty">Audito įrašų nerasta.</td>
				</tr>
			{:else}
				{#each auditLogListItems as log (log.id)}
					<tr
						class:tableRowAdmin={purpose === 'admin'}
						onclick={(e) => {
							if (purpose !== 'admin') return;
							e.stopPropagation();
							onSelectLog?.(log.id);
						}}
					>
						<td data-label="Laiko žyma">{formatDate(log.createdAt)}</td>
						{#if purpose === 'admin'}
							<td data-label="El. paštas / Naudotojo ID"
								><span
									>{@html highlightText(
										(log as AdminAuditLogListItem).actorEmail ??
											(log as AdminAuditLogListItem).userId ??
											'Nežinomas',
										searchTerm
									)}</span
								></td
							>
						{/if}
						<td data-label="Lygis"
							><span
								class="badge"
								class:info={log.level === 'INFO'}
								class:alert={log.level === 'ALERT'}
								><span>{purpose === 'user' ? translateLogLevel(log.level) : log.level}</span></span
							></td
						>
						<td data-label="Veiksmas"
							><span class="action"
								>{purpose === 'user' ? translateLogAction(log.actionType) : log.actionType}</span
							></td
						>
						<td data-label="Aprašymas"
							><span>{@html highlightText(log.message, searchTerm)}</span></td
						>
						<td data-label="Vientisumas"
							><span class="hashValid badge" class:valid={log.hashValid}
								>{log.hashValid ? 'Vientisas' : 'Pažeistas'}</span
							></td
						>
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

	td[data-label='El. paštas / Naudotojo ID'] {
		min-width: 0;
		white-space: normal;
		overflow-wrap: anywhere;
	}

	tbody .tableRowAdmin:hover {
		background: rgba(0, 0, 0, 0.03);
		cursor: pointer;
	}

	table td,
	table th {
		vertical-align: middle;
	}

	.badge {
		padding: 0.25rem 0.65rem;
		border-radius: 7px;
		font-size: 0.8rem;
		font-weight: 600;
	}

	.info {
		background: #e0f2fe;
		color: #0369a1;
	}

	.alert,
	.hashValid {
		background: #ffedd5;
		color: #c2410c;
	}

	.hashValid.valid {
		background: #dcfce7;
		color: #15803d;
	}

	.action {
		color: #374151;
		font-weight: 500;
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
