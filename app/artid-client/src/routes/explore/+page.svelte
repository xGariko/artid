<script lang="ts">
	import { toast } from 'svelte-sonner';
	import { api } from '$lib/api/browser-client';
	import type { PublicProfile } from '$lib/api/types';
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import ExploreResultsSection from '$lib/components/pages/explore/explore-results-section.svelte';
	import ExploreSearchSection from '$lib/components/pages/explore/explore-search-section.svelte';

	let searchQuery = $state('');
	let profiles = $state<PublicProfile[]>([]);
	let hasSearched = $state(false);

	// Cerca profili pubblici per nome/cognome, professione o titolo di un ArtID pubblico.
	async function runSearch(): Promise<void> {
		try {
			const { data, error } = await api.GET('/api/users/search', {
				params: { query: { query: searchQuery.trim() } }
			});

			hasSearched = true;

			if (error || !data) {
				toast.error('Errore durante la ricerca dei profili');
				profiles = [];
				return;
			}

			profiles = data;
		} catch {
			hasSearched = true;
			toast.error('Errore durante la ricerca dei profili');
			profiles = [];
		}
	}
</script>

<ArtidNavbar></ArtidNavbar>

<div class="w-100 h-100 d-flex flex-column align-items-center justify-content-start p-5 explore-main-container gap-3">
	<ExploreSearchSection bind:searchQuery onSearch={runSearch} />
	<ExploreResultsSection {profiles} {hasSearched} />
</div>


<style>
    .explore-main-container {
        padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
    }
</style>