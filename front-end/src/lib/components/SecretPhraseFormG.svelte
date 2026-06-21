<script lang="ts">
	import { X } from 'lucide-svelte';
	import { createVaultRecord, API_BASE, getVaultStatus, getMe, toast, getErrorMessage } from '$lib';
	import { onMount } from 'svelte';

	let secretPhrase = $state<string>('');
	let secretPhraseR = $state<string>('');
	let triedSubmit: boolean = false;
	let loading = $state<boolean>(false);
	let showDialog = $state<boolean>(false);
	let errors = $state<{ secretP?: string; secretPR?: string }>({});

	onMount(async () => {
		try {
			const status = await getVaultStatus();
			if (status === 'not_initialized') {
				showDialog = true;
				toast.info('Šifravimo infrastruktūra dar neparuošta. Įveskite šifravimo slaptažodį.');
			}
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko nustatyti naudotojo raktų būsenos!');
		}
	});

	async function generateKeys() {
		validate();
		if (Object.keys(errors).length !== 0) return;

		try {
			loading = true;

			const userId = (await getMe()).userId;
			const vaultRecord = await createVaultRecord(secretPhrase, userId);

			const res = await fetch(`${API_BASE}/vault/create`, {
				method: 'POST',
				credentials: 'include',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(vaultRecord)
			});

			if (!res.ok) {
				const message = await getErrorMessage(res, 'Nepavyko sugeneruoti šifravimo raktų!');
				throw new Error(message);
			}

			showDialog = false;
			toast.success('Šifravimo raktai sėkmingai sugeneruoti!');
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Nepavyko sugeneruoti šifravimo raktų!');
		} finally {
			loading = false;
		}
	}

	function validate() {
		errors = {};
		triedSubmit = true;

		if (!secretPhrase.trim()) {
			errors.secretP = 'Slaptažodis yra privalomas!';
		} else if (secretPhrase.length < 12) {
			errors.secretP = 'Slaptažodis turi būti bent 12 simbolių ilgio!';
		}

		if (!secretPhraseR.trim()) {
			errors.secretPR = 'Pakartokite slaptažodį!';
		} else if (secretPhrase !== secretPhraseR) {
			errors.secretPR = 'Slaptažodžiai nesutampa!';
		}
	}

	function maybeValidate() {
		if (triedSubmit) validate();
	}

	function onClose() {
		showDialog = false;
		errors = {};
	}
</script>

{#if showDialog}
	<div class="overlay">
		<div class="form">
			<button class="close" type="button" onclick={onClose}><X size={20} /></button>
			<header>
				<h2>Sukurkite šifravimo slaptažodį</h2>
				<p>Šis slaptažodis bus naudojamas jūsų pagrindiniam šifravimo raktui apsaugoti.</p>
			</header>
			<div class="body">
				<div class="warning">
					<span>⚠️</span>
					<div>
						<strong>Svarbu</strong>
						<span class="warn-text"
							>Jei slaptažodį pamiršite, užšifruotų duomenų atkurti nepavyks!</span
						>
					</div>
				</div>
				<div class="input-wrapper">
					<div class="field">
						<label for="passphrase">Šifravimo slaptažodis</label>
						<input
							id="passphrase"
							type="password"
							placeholder="Įveskite stiprų slaptažodį"
							minlength="12"
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
					<div class="field">
						<label for="confirm">Patvirtinkite slaptažodį</label>
						<input
							id="confirm"
							type="password"
							placeholder="Pakartokite slaptažodį"
							minlength="12"
							bind:value={secretPhraseR}
							oninput={maybeValidate}
							required
						/>
						{#if errors.secretPR}
							<p class="error-text">
								{errors.secretPR}
							</p>
						{/if}
					</div>
				</div>
			</div>
			<div class="footer">
				<button class="submit" type="button" onclick={generateKeys} disabled={loading}
					>{loading ? 'Generuojami raktai...' : 'Išsaugoti slaptažodį'}</button
				>
			</div>
		</div>
	</div>
{/if}

<style>
	.overlay {
		background: rgba(15, 23, 42, 0.45);
	}

	.form {
		position: relative;
		max-width: 520px;
		width: 100%;
		background:
			linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03)),
			radial-gradient(900px 400px at 50% 0%, rgba(91, 108, 255, 0.14), transparent 55%),
			var(--panel);
		overflow: hidden;
		border-radius: var(--r-1);
		border: 1px solid var(--line-2);
		overflow: hidden;
	}

	.form * {
		color: var(--text);
	}

	header {
		padding: 18px 20px 14px;
		background: rgba(10, 14, 30, 0.4);
		border-bottom: 1px solid var(--line);
	}

	header h2 {
		font-size: 1.2rem;
		font-weight: 500;
	}

	header p {
		margin: 0.4rem 0 0;
		color: #000;
		font-size: 0.7rem;
		opacity: 0.8;
	}

	.body {
		padding: 18px 20px 20px;
	}

	.warning {
		display: flex;
		gap: 0.5rem;
		padding: 5px 5px;
		border-radius: 5px;
		border: 1px solid rgba(255, 90, 106, 0.35);
		background: rgba(255, 90, 106, 0.1);
		margin-bottom: 16px;
		color: #000;
	}

	.warning strong {
		margin-bottom: 0.5rem;
		font-size: 0.85rem;
	}

	.warning span:first-child {
		margin-top: 0.1rem;
	}

	.warning span {
		display: block;
		font-size: 0.75rem;
		line-height: 1.35;
	}

	.warn-text {
		opacity: 0.8;
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
		color: #000;
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

	.footer {
		display: flex;
		justify-content: center;
		padding: 1rem;
		border-top: 1px solid var(--line);
		background: rgba(10, 14, 30, 0.4);
	}

	.close {
		position: absolute;
		top: 10px;
		right: 10px;
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
