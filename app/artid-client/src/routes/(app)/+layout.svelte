<script lang="ts">
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import ArtidSubNavbar from '$lib/components/layout/artid-sub-navbar.svelte';
	import { page } from '$app/state';

	let { children, data } = $props();
</script>

<ArtidNavbar propicUrl={data.propicUrl}></ArtidNavbar>
<ArtidSubNavbar></ArtidSubNavbar>
<main class="flex-grow-1 w-100 h-100 p-4 bg-artid-surface overflow-auto {page.url.pathname === '/dashboard' ? 'dashboard' : ''}">
	{@render children()}
</main>

<style>
	/*
	* Padding cambia in base alla presenza di subnavbar o meno
	*/
	main {
			padding-top: calc(var(--artid-navbar-height) + (var(--artid-navbar-height) /2 ) + 1.5rem) !important;
	}

	.dashboard{
      padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
	}
</style>