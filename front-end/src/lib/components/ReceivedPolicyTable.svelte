<script lang="ts">
	import { type ReceivedPolicyListItem } from '$lib/policies/policyTypes';
	import { formatDate } from '$lib/dateFormat';
	import { FileIcon } from 'lucide-svelte';
	import { highlightText } from '$lib/api';

	type Props = {
		initLoading: boolean;
		receivedPolicyListItems: ReceivedPolicyListItem[];
		error: string | null;
		searchTerm: string;
		onSelectPolicy: (policyId: string) => void;
	};

	let { receivedPolicyListItems, initLoading, error, searchTerm, onSelectPolicy }: Props = $props();
</script>

<div class="table-wrap">
	<table>
		<thead>
			<tr>
				<th class="name">Politikos pavadinimas</th>
				<th class="released-by">Atskleidė</th>
				<th>Failai</th>
				<th class="released-at">Atskleista</th>
				<th>Būsena</th>
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
			{:else if receivedPolicyListItems.length === 0}
				<tr>
					<td colspan="5" class="empty">Gautų politikų nėra.</td>
				</tr>
			{:else}
				{#each receivedPolicyListItems as policy (policy.policyId)}
					{#if policy.loading === true}
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
						<tr
							onclick={(e) => {
								e.stopPropagation();
								onSelectPolicy(policy.policyId);
							}}
						>
							<td class="name">
								<span class="file-icon"><FileIcon size={20} /></span>
								<span class="policy-name">{@html highlightText(policy.policyName, searchTerm)}</span
								>
							</td>
							<td class="released-by" data-label={'Atskleidė'}><span>{policy.releasedBy}</span></td>
							<td data-label={'Failai'}><span>{policy.filesCount}</span></td>
							<td class="released-at" data-label={'Atskleista'}
								><span>{formatDate(policy.releasedAt)}</span></td
							>
							<td data-label={'Būsena'}>
								<span class="pill" data-status={policy.viewed ? 'VIEWED' : 'NEW'}
									><span>{policy.viewed ? 'PERŽIŪRĖTA' : 'NAUJA'}</span>
								</span>
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

	tbody tr:hover {
		background: rgba(0, 0, 0, 0.03);
		cursor: pointer;
	}
	table td,
	table th {
		vertical-align: middle;
	}

	td.name,
	th.name {
		display: flex;
		align-items: center;
		gap: 5px;
		min-width: 0;
	}

	.file-icon {
		display: flex;
	}

	.policy-name {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		min-width: 0;
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

	@media (max-width: 1000px) {
		td.released-by {
			overflow-wrap: anywhere;
		}
	}
</style>
