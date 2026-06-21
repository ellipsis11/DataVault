<script lang="ts">
	import SideBar from '$lib/components/SideBar.svelte';
	import Header from '$lib/components/Header.svelte';

	let { children, data } = $props();

	let mobileSidebarOpened = $state<boolean>(false);
	let innerWidth = $state(0);

	function toggleMobileSideBar() {
		mobileSidebarOpened = !mobileSidebarOpened;
	}

	$effect(() => {
		if (innerWidth > 600) {
			mobileSidebarOpened = false;
		}
	});
</script>

<svelte:window bind:innerWidth />

<div class="layout">
	<SideBar {data} {mobileSidebarOpened} {toggleMobileSideBar} />
	<div class="main-content">
		<Header {data} {toggleMobileSideBar} />
		{@render children()}
	</div>
</div>
