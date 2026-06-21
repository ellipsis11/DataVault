<script lang="ts">
	import { CloudUpload } from 'lucide-svelte';
	import { toast, getVaultStatus } from '$lib';

	type Props = {
		onFiles: (files: File[], status?: 'initialized' | 'not_initialized') => void;
	};

	let { onFiles }: Props = $props();

	let isOver = $state<boolean>(false);

	async function handleFiles(files: File[] | null) {
		try {
			const pickedFiles = files ?? [];
			if (pickedFiles.length === 0) return;
			const status = await getVaultStatus();
			onFiles(pickedFiles, status);
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Įvyko klaida!');
		}
	}
	function onDrop(e: DragEvent) {
		e.preventDefault();
		isOver = false;
		handleFiles(Array.from(e.dataTransfer?.files ?? []));
	}
</script>

<div
	class="upload"
	class:over={isOver}
	role="region"
	aria-label="File drop area"
	ondragenter={(e) => {
		e.preventDefault();
		isOver = true;
	}}
	ondragover={(e) => {
		e.preventDefault();
		isOver = true;
	}}
	ondragleave={(e) => {
		e.preventDefault();
		isOver = false;
	}}
	ondrop={(e) => onDrop(e)}
>
	<div class="uplaod-text">
		<CloudUpload size={30} color="#cd1222" />
		<p>Įkelkite failus nutempdami juos čia</p>
	</div>
	<label class="upload-btn">
		<input
			type="file"
			multiple
			hidden
			onchange={(e) => {
				const input = e.currentTarget as HTMLInputElement;
				const pickedFiles = Array.from(input.files ?? []);
				input.value = '';
				handleFiles(pickedFiles);
			}}
		/>arba <span>pasirinkite failus</span>
	</label>
</div>

<style>
	.upload {
		background-color: #efedf6;
		text-align: center;
		padding: 2.5rem 0.6rem 1.25rem 0.62rem;
		margin-top: 0.3125rem;
		border-radius: 7px;
		border: 2px dashed #d1d5db;
	}

	.upload.over {
		border-color: #3b82f6;
		background: rgba(59, 130, 246, 0.08);
	}

	.uplaod-text {
		display: flex;
		justify-content: center;
		align-items: center;
		font-size: 1.3rem;
		margin-bottom: 1rem;
	}

	.uplaod-text > :last-child {
		margin-left: 0.93rem;
	}

	.upload-btn {
		background-color: #fbfbfb50;
		padding: 0.31rem 0.93rem;
		border: 1px solid #cdcdcd;
		border-radius: 7px;
		font-size: 0.9rem;
		cursor: pointer;
	}

	.upload-btn span {
		color: #cd1222;
		font-weight: 700;
	}
</style>
