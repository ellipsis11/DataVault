<script lang="ts">
	import { X } from 'lucide-svelte';

	let secretPhrase = $state<string>('');
	let {
		onSubmit,
		onClose,
		loading = false
	} = $props<{
		onSubmit: (v: string) => void;
		onClose: (errorText?: string) => void;
		loading: boolean;
	}>();
	let triedSubmit: boolean = false;
	let errors = $state<{ secretP?: string }>({});

	function validate(): void {
		errors = {};
		triedSubmit = true;

		if (!secretPhrase.trim()) {
			errors.secretP = 'Įveskite šifravimo slaptažodį!';
		}
	}

	function maybeValidate(): void {
		if (triedSubmit) validate();
	}

	function submit(): void {
		validate();

		if (Object.keys(errors).length > 0) {
			return;
		}

		onSubmit(secretPhrase);
	}
</script>

<div class="overlay">
	<div class="form">
		<header>
			<h2>Pateikite šifravimo slaptažodį</h2>
			<button
				class="close"
				type="button"
				onclick={() => onClose('Slaptažodžio įvedimas buvo atšauktas!')}><X size={20} /></button
			>
		</header>
		<div class="body">
			<div class="input-wrapper">
				<div class="field">
					<label for="passphrase">Šifravimo slaptažodis</label>
					<input
						id="passphrase"
						type="password"
						placeholder="Įveskite slaptažodį"
						bind:value={secretPhrase}
						oninput={maybeValidate}
						required
					/>
					{#if errors.secretP}
						<p class="error-text">
							{errors.secretP}
						</p>
					{/if}
				</div>
			</div>
		</div>
		<div class="footer">
			<button class="submit" type="button" onclick={submit} disabled={loading}
				>{loading ? 'Generuojami raktai...' : 'Pateikti'}</button
			>
		</div>
	</div>
</div>

<style>
	.overlay {
		background: rgba(15, 23, 42, 0.45);
	}

	.form {
		position: relative;
		max-width: 400px;
		width: 100%;
		background:
			linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03)),
			radial-gradient(900px 400px at 50% 0%, rgba(91, 108, 255, 0.14), transparent 55%),
			var(--panel);
		overflow: hidden;
		border-radius: var(--r-1);
		border: 1px solid var(--line-2);
	}

	.form * {
		color: var(--text);
	}

	header {
		display: flex;
		justify-content: space-between;
		padding: 0.75rem 1.125rem 0.75rem;
		background: rgba(10, 14, 30, 0.4);
		border-bottom: 1px solid var(--line);
	}

	header h2 {
		font-size: 1.2rem;
	}

	.body {
		padding: 18px 20px 20px;
	}

	.input-wrapper {
		display: flex;
		justify-content: space-between;
		gap: 0.5rem;
	}

	.field {
		width: 100%;
	}

	label {
		display: block;
		font-size: 13px;
		color: rgba(255, 255, 255, 0.86);
		margin: 0 0 6px;
	}

	input {
		width: 100%;
		padding: 12px 12px;
		border-radius: 7px;
		border: 1px solid rgba(255, 255, 255, 0.16);
		background: rgba(10, 12, 24, 0.55);
		color: rgba(255, 255, 255, 0.92);
		outline: none;
	}

	input::placeholder {
		color: rgba(255, 255, 255, 0.45);
	}

	input:focus {
		border-color: rgb(28, 28, 28);
	}

	.error-text {
		position: absolute;
		color: red;
		font-size: 10px;
		margin-left: 3px;
		margin-top: 3px;
	}

	.footer {
		display: flex;
		justify-content: center;
		padding: 1rem;
		border-top: 1px solid var(--line);
		background: rgba(10, 14, 30, 0.4);
	}

	.submit {
		border: 1px solid rgba(255, 255, 255, 0.16);
		background: rgba(39, 45, 158, 0.861);
		color: rgba(255, 255, 255, 0.88);
		padding: 10px 14px;
		border-radius: 7px;
		font-weight: 600;
	}

	.submit:hover {
		background: rgb(39, 45, 158);
	}

	.submit:active {
		transform: translateY(1px);
	}

	.close {
		width: 30px;
		height: 30px;
		border-radius: 7px;
		display: flex;
		justify-content: center;
		align-items: center;
		border: 1px solid rgba(255, 255, 255, 0.14);
		background: rgba(255, 255, 255, 0.06);
		color: rgba(255, 255, 255, 0.85);
	}
</style>
