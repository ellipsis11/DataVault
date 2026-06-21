<script lang="ts">
	import { ChevronLeft, ChevronRight } from 'lucide-svelte';

	type Props = {
		page: number;
		size: 10 | 20 | 50 | 100;
		total: number;
		onSizeChange: (size: 10 | 20 | 50 | 100) => void;
		onPageChange: (page: number) => void;
	};

	let { page, size, total, onSizeChange, onPageChange }: Props = $props();
	let from = $derived(total === 0 ? 0 : page * size + 1);
	let to = $derived(Math.min((page + 1) * size, total));
	let totalPages = $derived(Math.ceil(total / size));

	let visiblePages = $derived.by(() => {
		const current = page;
		const last = totalPages - 1;

		if (totalPages <= 6) {
			return Array.from({ length: totalPages }, (_, i) => i);
		}

		const pages: (number | 'dots')[] = [];

		pages.push(0);

		let start = Math.max(1, current - 3);
		let end = Math.min(last - 1, current + 3);

		if (start > 1) {
			pages.push('dots');
		}

		for (let i = start; i <= end; i++) {
			pages.push(i);
		}

		if (end < last - 1) {
			pages.push('dots');
		}

		pages.push(last);

		return pages;
	});

	async function prevPage() {
		if (page <= 0) return;
		page -= 1;
		onPageChange(page);
	}

	async function nextPage() {
		if (page >= totalPages - 1) return;
		page += 1;
		onPageChange(page);
	}
</script>

{#if total > 0}
	<div class="table-pagination">
		<div class="left">
			<select class="perpage" bind:value={size} onchange={() => onSizeChange(size)}>
				<option value={10}>10 per puslapį</option>
				<option value={20}>20 per puslapį</option>
				<option value={50}>50 per puslapį</option>
				<option value={100}>100 per puslapį</option>
			</select>
			<span class="range">{`${from}-${to} iš ${total}`}</span>
		</div>
		<div class="right">
			<button class="nav" aria-label="Previous page" onclick={prevPage} disabled={page <= 0}>
				<ChevronLeft />
			</button>
			<div class="pages">
				{#each visiblePages as p}
					{#if p === 'dots'}
						<span class="dots">...</span>
					{:else}
						<button
							class="page"
							class:active={page === p}
							type="button"
							onclick={() => {
								onPageChange(p);
								page = p;
							}}
						>
							{p + 1}</button
						>
					{/if}
				{/each}
			</div>
			<div class="pages mobile">
				<span>{page + 1} / {totalPages}</span>
			</div>
			<button
				class="nav"
				aria-label="Next page"
				onclick={nextPage}
				disabled={page >= totalPages - 1}
			>
				<ChevronRight />
			</button>
		</div>
	</div>
{/if}

<style>
	.table-pagination {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 1rem;
	}

	.left,
	.right {
		display: flex;
		align-items: center;
		gap: 14px;
	}

	.perpage {
		height: 1.875rem;
		padding: 0 14px;
		border: 1px solid #00000012;
		border-radius: 7px;
		box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
		cursor: pointer;
	}

	.range {
		margin-left: 0.5rem;
		font-size: 0.8rem;
		font-weight: 600;
	}

	.pages {
		display: flex;
		align-items: center;
		gap: 0.3rem;
	}

	button {
		display: flex;
		justify-content: center;
		align-items: center;
		width: 1.875rem;
		height: 1.875rem;
		border: 1px solid #00000012;
		border-radius: 7px;
		background: #fff;
		cursor: pointer;
		box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
	}

	button:disabled {
		cursor: not-allowed;
	}

	.page.active {
		background: #e5e7eb;
		border-color: #00000012;
	}

	.dots {
		color: #6b7280;
		padding: 0 8px;
		user-select: none;
	}

	.pages.mobile {
		display: none;
	}

	@media (max-width: 600px) {
		.range {
			display: none;
		}

		.pages:not(.mobile) {
			display: none;
		}

		.pages.mobile {
			display: flex;
		}
	}
</style>
