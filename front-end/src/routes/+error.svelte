<script lang="ts">
	// Contains info about the current route
	import { page } from '$app/state';
</script>

<div class="error-page">
	<div class="error-card">
		<p class="logo">DataVault</p>
		<h1>{page.status}</h1>
		<h2>
			{page.error?.message ?? 'Įvyko nenumatyta klaida.'}
		</h2>
		<a href="/">Grįžti į pradžią</a>
	</div>
</div>

<style>
	.error-page {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100vh;
		padding: 2rem;
		color: #e5e7eb;
	}

	.error-card {
		text-align: center;
	}

	.logo {
		margin-bottom: 1rem;
		font-size: 0.8rem;
		letter-spacing: 0.16em;
		text-transform: uppercase;
		color: #00214f;
	}

	h1 {
		margin: 0;
		font-size: 5rem;
		line-height: 1;
		color: #60a5fa;
	}

	h2 {
		margin: 1rem 0 0.5rem;
		font-size: 1.5rem;
		color: #05244f;
	}

	a {
		display: inline-flex;
		margin-top: 1rem;
		padding: 0.5rem;
		border-radius: 12px;
		background: #2563eb;
		color: white;
		text-decoration: none;
		font-size: 0.9rem;
		font-weight: 600;
	}

	a:hover {
		background: #1d4ed8;
	}
</style>
