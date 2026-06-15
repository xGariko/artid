<script lang="ts">
	import type { PublicProfile } from '$lib/api/types';
	import ExploreProfileCard from './explore-profile-card.svelte';
	import ExplorePagination from './explore-pagination.svelte';

	let { profiles, hasSearched }: { profiles: PublicProfile[]; hasSearched: boolean } = $props();

	const PAGE_SIZE = 10;

	let currentPage = $state(1);

	const totalPages = $derived(Math.max(1, Math.ceil(profiles.length / PAGE_SIZE)));

	// Nuovi risultati → torna a pagina 1 (evita di restare su una pagina ormai inesistente).
	$effect(() => {
		currentPage = 1;
	});

	const pageProfiles = $derived(
		profiles.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
	);

	function goToPage(targetPage: number): void {
		currentPage = Math.min(Math.max(targetPage, 1), totalPages);
	}
</script>

<div class="bg-artid-section border border-artid-border rounded-3 w-75 h-75 p-3 d-flex flex-column results-section">
	{#if pageProfiles.length > 0}
		<div class="row row-cols-2 row-cols-sm-3 row-cols-lg-4 row-cols-xl-5 g-3">
			{#each pageProfiles as profile (profile.id)}
				<div class="col">
					<ExploreProfileCard {profile} />
				</div>
			{/each}
		</div>

		<ExplorePagination {currentPage} {totalPages} onChange={goToPage} class="mt-auto pt-4" />
	{:else}
		<div
			class="flex-grow-1 d-flex flex-column align-items-center justify-content-center text-artid-text-muted text-center px-3"
		>
			{#if hasSearched}
				<i class="bi bi-search fs-1 mb-3"></i>
				<span>Nessun profilo pubblico trovato</span>
			{:else}
				<i class="bi bi-people fs-1 mb-3"></i>
				<span>Cerca per nome, professione o ArtID per scoprire i profili pubblici</span>
			{/if}
		</div>
	{/if}
</div>

<style>
	/* Box risultati con altezza minima stabile: nessuna utility Bootstrap per un min-height in px. */
	.results-section {
		min-height: 500px;
	}
</style>
