<script lang="ts">
	let { json, padding, fontSize } = $props<{
		json: Record<string, unknown> | null;
		padding?: number;
		fontSize?: number;
	}>();
</script>

<div class="json-box">
	{#if json}
		<pre
			style:padding={`${padding ?? 1}rem`}
			style:font-size={`${fontSize ?? 0.9}rem`}>{JSON.stringify(json, null, 2)}</pre>
	{:else}
		<span
			class="empty-value"
			style:padding={`${padding ?? 1}rem`}
			style:font-size={`${fontSize ?? 0.9}rem`}>Metaduomenų nėra</span
		>
	{/if}
</div>

<style>
	.json-box {
		background-color: #fcfdfd;
		border: 1px solid #e5e7eb;
		border-radius: 7px;
	}

	.json-box pre {
		margin: 0;
		padding: 1rem;
		font-family: Consolas, 'Courier New', monospace;
		font-size: 0.9rem;
		font-weight: 300;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.empty-value {
		display: flex;
		justify-content: center;
		padding: 1rem;
		font-style: italic;
		font-weight: 400;
	}
</style>
