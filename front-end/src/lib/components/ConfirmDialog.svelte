<script lang="ts">
	import { TriangleAlert } from 'lucide-svelte';
	let { onClose, onConfirm, title, message, cancelLabel, confirmLabel } = $props<{
		onClose: () => void;
		onConfirm: () => void;
		title: string;
		message: string;
		cancelLabel: string;
		confirmLabel: string;
	}>();
</script>

<div class="overlay">
	<div class="modal">
		<div class="header">
			<div class="icon"><TriangleAlert /></div>
			<div class="text">
				<h2>{title}</h2>
				<p>{message}</p>
			</div>
		</div>
		<div class="actions">
			<button type="button" class="cancel-btn" onclick={() => onClose()}>{cancelLabel}</button>
			<button type="button" class="confirm-btn" onclick={() => onConfirm()}>{confirmLabel}</button>
		</div>
	</div>
</div>

<style>
	.overlay {
		position: fixed;
		inset: 0;
		z-index: 9998;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(15, 23, 42, 0.38);
	}

	.modal {
		width: 400px;
		border-radius: 10px;
		border: 1px solid rgba(255, 255, 255, 0.08);
		background: rgb(250, 250, 250);
		padding: 1.25rem;
	}

	.header {
		display: flex;
		gap: 0.9rem;
	}

	.icon {
		color: #f87171;
	}

	.text h2 {
		margin: 0 0 0.3rem 0;
		font-size: 1.2rem;
		font-weight: 700;
		line-height: 1.2;
	}

	.text p {
		margin: 0;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.7rem;
		margin-top: 1rem;
	}

	.cancel-btn,
	.confirm-btn {
		min-width: 110px;
		border-radius: 7px;
		padding: 0.5rem;
		font-size: 0.95rem;
		font-weight: 600;
		transition:
			transform 160ms ease,
			box-shadow 160ms ease,
			background-color 160ms ease;
	}

	.cancel-btn {
		border: 1px solid rgba(255, 255, 255, 0.12);
		background: rgba(255, 255, 255, 0.04);
	}

	.cancel-btn:hover {
		background: rgba(255, 255, 255, 0.08);
	}

	.confirm-btn {
		border: none;
		background: #ef4444f9;
		color: white;
	}

	.confirm-btn:hover {
		background: #dc2626;
	}

	.cancel-btn:hover,
	.confirm-btn:hover {
		transform: translateY(-1px);
	}
</style>
