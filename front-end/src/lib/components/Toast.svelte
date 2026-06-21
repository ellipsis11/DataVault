<script lang="ts">
	import { fly } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { toast } from '../ui/toast';
	import { X } from 'lucide-svelte';

	const icon = { success: '✓', error: '!', info: 'i' };
</script>

<!-- Svelte store – reactive variable -->
{#if $toast.open}
	<div class="wrap">
		<div
			class={'toast toast-' + $toast.type}
			in:fly={{ y: -30, duration: 300, easing: cubicOut }}
			out:fly={{ y: -30, duration: 300, easing: cubicOut }}
			on:click={() => toast.hide()}
			title="Click to dismiss"
			role="button"
			tabindex="0"
			on:keydown={(e) => {
				if (e.key === 'Enter' || e.key === ' ') {
					toast.hide();
				}
			}}
		>
			<span class="icon">{icon[$toast.type]}</span>
			<span class="message">{$toast.message}</span>
			<!-- For not calling hide function for the second time -->
			<button class="exit-btn" on:click|stopPropagation={() => toast.hide()}><X size={18} /></button
			>
		</div>
	</div>
{/if}

<style>
	.wrap {
		position: fixed;
		top: 14px;
		left: 50%;
		transform: translateX(-50%);
		width: min(520px, calc(100vw - 24px));
		z-index: 9999;
		padding: 0 1rem;
	}

	.toast {
		display: grid;
		grid-template-columns: 28px 1fr 34px;
		align-items: center;
		gap: 10px;
		padding: 12px 12px 12px 14px;
		border-radius: 7px;
		border: 1px solid rgba(255, 255, 255, 0.1);
		box-shadow: 0 5px 16px rgba(0, 0, 0, 0.155);
		color: rgba(255, 255, 255, 0.9);
	}

	.toast-success {
		background-color: rgba(13, 255, 134, 0.95);
	}

	.toast-error {
		background-color: rgba(255, 90, 120, 0.95);
	}

	.toast-info {
		background-color: rgba(110, 190, 255, 0.95);
	}

	.icon {
		width: 28px;
		height: 28px;
		border-radius: 7px;
		display: flex;
		justify-content: center;
		align-items: center;
		background: rgba(255, 255, 255, 0.08);
		color: rgba(255, 255, 255, 0.92);
		font-weight: 700;
	}

	.message {
		font-size: 0.85rem;
		color: rgb(255, 255, 255);
		font-weight: 600;
	}

	.exit-btn {
		border: none;
		background: transparent;
		color: rgba(255, 255, 255, 0.7);
		font-size: 20px;
		cursor: pointer;
	}
	.exit-btn:hover {
		background: rgba(255, 255, 255, 0.08);
		color: rgba(255, 255, 255, 0.92);
	}
</style>
