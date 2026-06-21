<script lang="ts">
	import { page } from '$app/state';
	import { toast } from '$lib/ui/toast';
	import {
		Box,
		Database,
		LayoutDashboard,
		Rocket,
		Logs,
		Settings,
		LogOut,
		Inbox,
		User,
		X
	} from 'lucide-svelte';
	import { onMount } from 'svelte';
	import {
		receivedPoliciesCount,
		newReceivedPoliciesCount,
		loadReceivedPoliciesCounts
	} from '../stores/receivedPolicies';

	let { data, mobileSidebarOpened, toggleMobileSideBar } = $props();
	const user = $derived(data.user);
	const currentPage = (urlEnd: string): boolean => page.url.pathname.startsWith(urlEnd);

	onMount(async () => {
		try {
			await loadReceivedPoliciesCounts();
		} catch (e) {
			toast.error(e instanceof Error ? e.message : 'Unknown error');
		}
	});

	function closeSidebarOnMobile() {
		if (mobileSidebarOpened) {
			toggleMobileSideBar();
		}
	}
</script>

{#if mobileSidebarOpened}
	<div class="overlay"></div>
{/if}
<div class="sidebar" class:mobile={mobileSidebarOpened}>
	<div class="top">
		<div class="logo">
			<Box />
			<span>DataVault</span>
			<button class="exit-btn" onclick={toggleMobileSideBar}><X size={20} /></button>
		</div>
	</div>
	<hr />
	<ul class="nav">
		{#if user.role === 'USER'}
			<li class:active={currentPage('/dashboard')}>
				<a href="/dashboard" onclick={closeSidebarOnMobile}
					><LayoutDashboard /><span>Pagrindinis</span></a
				>
			</li>
			<li class:active={currentPage('/file-storage')}>
				<a href="/file-storage" onclick={closeSidebarOnMobile}
					><Database /><span>Failų saugykla</span></a
				>
			</li>
			<li class:active={currentPage('/conditional-release')}>
				<a href="/conditional-release" onclick={closeSidebarOnMobile}
					><Rocket /><span>Sąlyginis atskleidimas</span></a
				>
			</li>
			{#if $receivedPoliciesCount > 0}
				<li class:active={currentPage('/received-files')}>
					<a href="/received-files" onclick={closeSidebarOnMobile}
						><Inbox /><span>Gautos politikos</span>
						{#if $newReceivedPoliciesCount > 0}
							<span class="newPolicies-tag">{$newReceivedPoliciesCount}</span>
						{/if}
					</a>
				</li>
			{/if}
			<li class:active={currentPage('/logs')}>
				<a href="/logs" onclick={closeSidebarOnMobile}><Logs /><span>Audito registrai</span></a>
			</li>
		{/if}
		{#if user.role === 'ADMIN'}
			<li class:active={currentPage('/admin/dashboard')}>
				<a href="/admin/dashboard" onclick={closeSidebarOnMobile}
					><LayoutDashboard /><span>Pagrindinis</span></a
				>
			</li>
			<li class:active={currentPage('/admin/users')}>
				<a href="/admin/users" onclick={closeSidebarOnMobile}><User /><span>Naudotojai</span></a>
			</li>
			<li class:active={currentPage('/admin/logs')}>
				<a href="/admin/logs" onclick={closeSidebarOnMobile}
					><Logs /><span>Audito registrai</span></a
				>
			</li>
		{/if}
	</ul>
	<ul class="settings">
		<li>Paskyra</li>
		{#if user.role === 'USER'}
			<li class:active={currentPage('/settings')}>
				<a href="/settings" onclick={closeSidebarOnMobile}><Settings /><span>Nustatymai</span></a>
			</li>
		{/if}
		<li>
			<form method="POST" action="/logout">
				<button type="submit" onclick={closeSidebarOnMobile}
					><LogOut /><span>Atsijungti</span></button
				>
			</form>
		</li>
	</ul>
</div>

<style>
	.sidebar {
		background-color: #fff;
		padding: 10px 25px;
		width: 250px;
		transition:
			width 0.25s ease,
			padding 0.25s ease;
	}

	.logo {
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 15px;
		margin-bottom: 15px;
	}

	.logo span {
		font-size: 1.2rem;
		font-weight: 700;
	}

	.logo .exit-btn {
		display: none;
		cursor: pointer;
	}

	hr {
		opacity: 0.3;
		margin-bottom: 30px;
	}

	.sidebar ul li {
		list-style: none;
		width: 100%;
	}

	.sidebar ul li:not(:last-child) {
		margin-bottom: 15px;
	}

	.sidebar ul li:hover,
	ul li.active {
		background-color: var(--gray);
		border-radius: 15px;
	}

	.sidebar ul li a,
	form button {
		width: 100%;
		position: relative;
		color: #000;
		display: flex;
		align-items: center;
		gap: 10px;
		text-decoration: none;
		font-weight: 500;
		font-size: 0.85rem;
		padding: 10px 20px;
	}

	.newPolicies-tag {
		background-color: rgba(232, 33, 33, 0.8);
		color: #fff;
		padding: 0.2rem 0.4rem;
		border-radius: 10px;
		font-size: 0.7rem;
	}

	.settings {
		margin-top: 1.25rem;
	}

	.settings li:first-child {
		margin-bottom: 0.938rem;
	}

	.settings li:first-child:hover {
		background-color: unset;
	}

	button {
		background-color: transparent;
		border: none;
	}

	ul li.active a::before {
		position: absolute;
		content: '';
		width: 4px;
		height: 70%;
		background-color: rgb(255, 34, 34);
		left: 7px;
		border-radius: 15px;
	}

	.overlay {
		position: fixed;
		inset: 0;
		width: 100vw;
		height: 100vh;
		background-color: rgba(0, 0, 0, 0.5);
		z-index: 999;
	}

	@media (max-width: 1000px) {
		.sidebar {
			display: block;
			width: 82px;
			padding: 10px;
		}

		.sidebar:not(.mobile) .logo span {
			display: none;
		}

		.sidebar:not(.mobile) .logo {
			justify-content: center;
			gap: 0;
		}

		.sidebar:not(.mobile) hr {
			display: none;
		}

		.sidebar:not(.mobile) .nav li span {
			display: none;
		}

		.sidebar:not(.mobile) .settings {
			margin-top: 2.25rem;
		}

		.sidebar:not(.mobile) .settings li:first-child {
			display: none;
		}

		.sidebar:not(.mobile) .settings li span {
			display: none;
		}
	}

	@media (max-width: 600px) {
		.sidebar {
			display: block;
			position: fixed;
			top: 0;
			left: 0;
			bottom: 0;
			width: 55%;
			min-width: 200px;
			z-index: 1000;
			transform: translateX(-100%);
			transition: transform 0.25s ease;
		}

		.sidebar.mobile {
			transform: translateX(0);
		}

		.sidebar.mobile hr {
			display: none;
		}

		.logo {
			display: flex;
			align-items: center;
			padding-right: 0.2rem;
		}

		.logo .exit-btn {
			display: flex;
			margin-left: auto;
		}
	}
</style>
