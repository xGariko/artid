<script>
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import ExploreSearchSection from '$lib/components/pages/explore/explore-search-section.svelte';

	let searchQuery = $state('');
</script>

<ArtidNavbar></ArtidNavbar>

<div class="w-100 h-100 d-flex align-items-start justify-content-center p-5 explore-main-container">
	<ExploreSearchSection
		{searchQuery}
	/>
</div>


<style>
    .explore-main-container {
        padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
    }

    .search-input {
        border-color: var(--artid-border);
    }

    .search-input:focus {
        border-color: var(--artid-primary);
        box-shadow: 0 0 0 0.2rem var(--artid-primary-subtle);
    }
</style>