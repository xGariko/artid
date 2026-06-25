<script lang="ts">
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import PublicArtidDetailView from '$lib/components/pages/explore/public-artid-detail-view.svelte';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<ArtidNavbar />

<div
	class="w-100 min-vh-100 d-flex flex-column align-items-center justify-content-start p-3 p-lg-5 artid-detail-container"
>
	<PublicArtidDetailView artid={data.artid} />
</div>

<style>
	.artid-detail-container {
		padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
		background-color: var(--artid-surface);
	}
</style>
