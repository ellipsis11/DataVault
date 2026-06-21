<script lang="ts">
	import { formatDate } from '$lib/dateFormat';
	import { Calendar, ChevronDown } from 'lucide-svelte';

	let { onDatePicked } = $props<{ onDatePicked: (fromDate: string, toDate: string) => void }>();
	let open = $state(false);
	let fromDate = $state<string>(new Date().toISOString().split('T')[0]);
	let toDate = $state<string>(
		new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split('T')[0]
	);
	let error = $state<string>();

	function validate() {
		error = '';
		if (!fromDate && !toDate) {
			error = 'At least one date needs to be picked!';
			return;
		}
		onDatePicked(fromDate, toDate);
	}
</script>

<div class="date-picker-btn">
	<button class="date-filter" type="button" onclick={() => (open = !open)}>
		<Calendar size={18} />
		<span>{`${formatDate(fromDate, 'date')} – ${formatDate(toDate, 'date')}`}</span>
		<ChevronDown size={16} />
	</button>

	{#if open}
		<div class="date-popup">
			<label>
				<span>Nuo</span>
				<input type="date" bind:value={fromDate} onchange={() => (error = '')} />
			</label>
			<label>
				<span>Iki</span>
				<input type="date" bind:value={toDate} onchange={() => (error = '')} />
			</label>
			{#if error?.trim()}
				<p class="error">{error}</p>
			{/if}
			<button type="button" class="apply-btn" onclick={validate}>Taikyti</button>
		</div>
	{/if}
</div>

<style>
	.date-picker-btn {
		display: inline-block;
		position: relative;
	}

	.date-filter {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.8rem 1rem;
		border: 1px solid #e5e7eb;
		border-radius: 0.7rem;
		background: white;
		font-size: 0.9rem;
		font-weight: 500;
		color: #111827;
		cursor: pointer;
	}

	.date-filter:hover {
		background: #f9fafb;
	}

	.date-filter span {
		white-space: nowrap;
	}

	.date-popup {
		display: flex;
		flex-direction: column;
		position: absolute;
		width: 260px;
		top: calc(100% + 0.5rem);
		right: 0;
		z-index: 20;
		gap: 0.75rem;
		padding: 1rem;
		border: 1px solid #e5e7eb;
		border-radius: 0.9rem;
		background: white;
		box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
	}

	.date-popup label {
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
		font-size: 0.8rem;
		font-weight: 600;
		color: #374151;
	}

	.date-popup input {
		padding: 0.65rem 0.75rem;
		border: 1px solid #d1d5db;
		border-radius: 0.55rem;
		font-size: 0.9rem;
	}

	.apply-btn {
		margin-top: 0.25rem;
		padding: 0.7rem 0.9rem;
		border: none;
		border-radius: 0.55rem;
		background: #111827;
		color: white;
		font-weight: 600;
		cursor: pointer;
	}

	.error {
		font-size: 0.7rem;
		color: red;
	}
</style>
