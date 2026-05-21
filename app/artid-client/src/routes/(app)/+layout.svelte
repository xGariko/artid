<script lang="ts">
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import ArtidProgress from '$lib/components/ui/artid-progress.svelte';

	let { children } = $props();


</script>

<ArtidNavbar></ArtidNavbar>
<ArtidProgress></ArtidProgress>
<main class="flex-grow-1 w-100 h-100 p-4 bg-artid-surface overflow-auto">
	{@render children()}
</main>

<style>
	main {
			padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
	}
</style>