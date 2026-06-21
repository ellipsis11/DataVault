<script lang="ts">
	import { Search } from 'lucide-svelte';
	import UserAvatar from './UserAvatar.svelte';
	import { Menu, Box } from 'lucide-svelte';

	let { data, toggleMobileSideBar } = $props();
	let user = $derived(data.user);

	const name = $derived(() => {
		if (!user.email) return '?';

		const username = user.email.split('@')[0].trim();
		if (!username) return '?';

		const parts = username.split(/[._-]/).filter(Boolean);

		return parts[0] ?? '?';
	});
</script>

<header>
	<div class="header header-left"><p>Sveiki sugrįžę, {name()}</p></div>
	<div class="header header-right">
		<UserAvatar email={user.email} width={3} height={3} fontSize={1.5} />
	</div>
</header>

<header class="mobile">
	<div class="header header-left">
		<button onclick={toggleMobileSideBar}><Menu /></button>
	</div>
	<div class="header header-center">
		<Box />
		<span>DataVault</span>
	</div>
	<div class="header header-right">
		<UserAvatar email={user.email} width={2} height={2} fontSize={1.2} />
	</div>
</header>

<style>
	header {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.header-left p {
		font-size: 1.5rem;
		font-weight: 500;
		padding: 0;
	}

	.header-right {
		max-width: fit-content;
		width: 100%;
		display: flex;
		justify-content: space-between;
		gap: 1rem;
		align-items: center;
	}

	button {
		background-color: transparent;
		border: none;
	}

	header.mobile {
		display: none;
	}

	@media (max-width: 600px) {
		header:not(.mobile) {
			display: none;
		}

		header.mobile {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 0.5rem 0.9rem;
			gap: 0.9rem;
			margin: -1.5rem -1.5rem 0rem -1.5rem;
			width: calc(100% + 3rem);
			background-color: #fff;
		}

		.header-center {
			display: flex;
			align-items: center;
			gap: 0.65rem;
			font-size: 1.2rem;
			font-weight: 700;
		}
	}
</style>
