<script lang="ts">
	import { goto } from '$app/navigation';
	import { API_BASE, toast, type AuthUser, getErrorMessage } from '$lib';
	import { onMount } from 'svelte';

	type Mode = 'Login' | 'Register';
	let mode = $state<Mode>('Login');

	let email = $state<string>('');
	let password = $state<string>('');
	let confirmPassword = $state<string>('');

	let loading = $state<boolean>(false);
	let triedSubmit: boolean = false;

	let errors = $state<{
		email?: string;
		password?: string;
		confirmPassword?: string;
		general?: string;
	}>({});

	onMount(() => (loading = false));

	function validate() {
		errors = {};
		triedSubmit = true;
		const e = email.trim();
		if (!e) errors.email = 'El. paštas reikalingas.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)) errors.email = 'El. pašto formatas netinkamas.';

		if (!password) errors.password = 'Slaptažodis yra privalomas.';
		else if (password.length < 8) errors.password = 'Slaptažodis turi būti bent 8 simbolių ilgio.';

		if (mode === 'Register') {
			if (!confirmPassword) errors.confirmPassword = 'Pakartokite slaptažodį.';
			else if (password !== confirmPassword) errors.confirmPassword = 'Slaptažodžiai nesutampa.';
		}
	}

	function maybeValidate() {
		if (triedSubmit) validate();
	}

	function onModeChange() {
		errors = {};
		triedSubmit = false;
		mode = mode === 'Register' ? 'Login' : 'Register';
		clearFields();
	}

	function clearFields() {
		email = password = confirmPassword = '';
	}

	async function submit() {
		validate();
		if (Object.keys(errors).length > 0) return;

		loading = true;

		try {
			const endPoint = mode === 'Login' ? '/login' : '/register';
			const url = API_BASE + '/auth' + endPoint;

			const res = await fetch(url, {
				method: 'POST',
				credentials: 'include',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					email,
					password
				})
			});

			if (res.ok) {
				if (mode === 'Login') {
					const user: AuthUser = await res.json();

					goto(user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard');

					clearFields();
				} else if (mode === 'Register') {
					toast.success('Registracija sėkminga');
					
					mode = 'Login';
					clearFields();
					loading = false;
				}
			} else {
				loading = false;
				const message = await getErrorMessage(
					res,
					`Įvyko klaida bandant ${mode === 'Login' ? 'prisijungti' : 'registruotis'}`
				);

				throw new Error(message);
			}
		} catch (e) {
			loading = false;
			toast.error(
				e instanceof Error
					? e.message
					: `Įvyko klaida bandant ${mode === 'Login' ? 'prisijungti' : 'registruotis'}`
			);
		} finally {
			triedSubmit = false;
		}
	}

	function callToOAuth(provider: string) {
		window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
	}
</script>

<svelte:head>
	<title>Autentifikacijos puslapis</title>
</svelte:head>

<div class="wrapper">
	<div class="login-container">
		<h1>{mode === 'Login' ? 'PRISIJUNGIMAS' : 'SUKURTI PASKYRĄ'}</h1>
		<div class="input-group">
			<label for="email">El. paštas</label>
			<input
				style={errors.email ? 'border: 2px solid red;' : ''}
				type="email"
				bind:value={email}
				oninput={maybeValidate}
				placeholder="jusu@pastas.lt"
				autocomplete="email"
				required
			/>
			{#if errors.email}
				<p class="error-text">{errors.email}</p>
			{/if}
		</div>
		<div class="input-group">
			<label for="password">Slaptažodis</label>
			<input
				style={errors.password ? 'border: 2px solid red;' : ''}
				type="password"
				bind:value={password}
				oninput={maybeValidate}
				placeholder="••••••••"
				autocomplete={mode === 'Login' ? 'current-password' : 'new-password'}
				required
			/>
			{#if errors.password}
				<p class="error-text">{errors.password}</p>
			{/if}
		</div>
		{#if mode === 'Register'}
			<div class="input-group">
				<label for="confirmPassword">Pakartokite slaptažodį</label>
				<input
					style={errors.confirmPassword ? 'border: 2px solid red;' : ''}
					type="password"
					bind:value={confirmPassword}
					oninput={maybeValidate}
					placeholder="••••••••"
					autocomplete="new-password"
					required
				/>
				{#if errors.confirmPassword}
					<p class="error-text">{errors.confirmPassword}</p>
				{/if}
			</div>
		{/if}
		<button class="submit-btn" type="button" disabled={loading} onclick={submit}
			>{loading ? 'Kraunama...' : mode === 'Login' ? 'Prisijungti' : 'Susikurti paskyrą'}</button
		>
		<div class="divider">ARBA</div>
		<div class="social-login">
			<div class="social-btn">
				<button type="button" onclick={() => callToOAuth('google')}>
					<img src="/img/google-logo.png" alt="Google Logo" width="24" height="24" />
					<span>Tęsti su Google</span>
				</button>
			</div>
			<div class="social-btn">
				<button type="button" onclick={() => callToOAuth('github')}>
					<img src="/img/github-logo.png" alt="GitHub Logo" width="24" height="24" />
					<span>Tęsti su GitHub</span></button
				>
			</div>
		</div>
		<div class="footer">
			<p>
				{mode === 'Register' ? 'Jau turite paskyrą?' : 'Neturite paskyros?'}
				<button type="button" onclick={onModeChange}
					>{mode === 'Register' ? 'Prisijungti' : 'Registruotis'}</button
				>
			</p>
		</div>
	</div>
</div>

<style>
	:root {
		--primary-color: #000000;
		--secondary-color: #ffffff;
		--accent-color: #ff5e5b;
		--shadow: 8px 8px 0px;
	}

	.wrapper {
		display: flex;
		justify-content: center;
		align-items: center;
		min-height: 100vh;
	}

	.login-container {
		width: 100%;
		max-width: 400px;
		border: 1px solid var(--primary-color);
		padding: 40px 30px;
		background-color: var(--secondary-color);
		box-shadow: var(--shadow);
		border-radius: 12px;
	}

	h1 {
		font-size: 28px;
		font-weight: 700;
		margin-bottom: 30px;
		text-align: center;
	}

	.input-group {
		margin-bottom: 20px;
	}

	label {
		display: block;
		font-weight: 700;
		margin-bottom: 8px;
		color: var(--primary-color);
	}

	input {
		width: 100%;
		padding: 12px 15px;
		border: 2px solid var(--primary-color);
		background-color: var(--secondary-color);
		font-size: 16px;
		outline: none;
		transition: all 0.3s;
	}

	input:focus {
		box-shadow: 4px 4px 0px var(--primary-color);
	}

	.submit-btn {
		width: 100%;
		padding: 12px;
		background-color: var(--accent-color);
		color: var(--secondary-color);
		font-size: 16px;
		font-weight: 700;
		cursor: pointer;
		margin-top: 10px;
		transition: all 0.3s;
	}

	.submit-btn:hover {
		box-shadow: 4px 4px 0px var(--primary-color);
		transform: translate(-2px, -2px);
	}

	.divider {
		display: flex;
		align-items: center;
		margin: 25px 0;
		color: var(--primary-color);
		font-weight: 700;
	}

	.divider::before,
	.divider::after {
		content: '';
		flex: 1;
		border-bottom: 2px solid var(--primary-color);
		margin: 0 10px;
	}

	.social-login {
		display: flex;
		justify-content: center;
		gap: 0.5rem;
	}

	.social-btn {
		display: flex;
		align-items: center;
		border: 2px solid var(--primary-color);
		cursor: pointer;
		transition: all 0.3s;
	}

	.social-btn button {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.5rem;
		background: transparent;
		border: none;
		cursor: pointer;
	}

	.social-btn span {
		font-size: 0.7rem;
	}

	.social-btn:hover {
		box-shadow: 4px 4px 0px var(--primary-color);
		transform: translate(-2px, -2px);
	}

	.footer {
		text-align: center;
		margin-top: 20px;
		color: var(--primary-color);
	}

	.footer button {
		display: inline-block;
		color: var(--primary-color);
		font-weight: 700;
		text-decoration: underline;
		background: transparent;
		border: none;
		cursor: pointer;
	}

	.footer button:hover {
		color: var(--accent-color);
	}

	.error-text {
		position: absolute;
		color: red;
		font-size: 10px;
		margin-left: 3px;
		margin-top: 3px;
	}

	@media (max-width: 600px) {
		.login-container {
			margin: 1rem;
		}
	}
</style>
