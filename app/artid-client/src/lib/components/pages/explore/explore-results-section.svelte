<script lang="ts">
	import type { PublicProfile } from '$lib/api/types';
	import ExploreProfileCard from './explore-profile-card.svelte';

	let { profiles, hasSearched }: { profiles: PublicProfile[]; hasSearched: boolean } = $props();

	const PAGE_SIZE = 10;
	const MAX_PAGE_BUTTONS = 5;

	let currentPage = $state(1);

	const totalPages = $derived(Math.max(1, Math.ceil(profiles.length / PAGE_SIZE)));

	// Nuovi risultati → torna a pagina 1 (evita di restare su una pagina ormai inesistente).
	$effect(() => {
		currentPage = 1;
	});

	const pageProfiles = $derived(
		profiles.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
	);

	// Finestra scorrevole di numeri pagina centrata sulla pagina corrente.
	const pageNumbers = $derived.by(() => {
		if (totalPages <= MAX_PAGE_BUTTONS) {
			return Array.from({ length: totalPages }, (_, index) => index + 1);
		}
		const half = Math.floor(MAX_PAGE_BUTTONS / 2);
		const end = Math.min(totalPages, Math.max(currentPage + half, MAX_PAGE_BUTTONS));
		const start = Math.max(1, end - MAX_PAGE_BUTTONS + 1);
		return Array.from({ length: end - start + 1 }, (_, index) => start + index);
	});

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

		{#if totalPages > 1}
			<nav class="mt-auto pt-4" aria-label="Paginazione risultati">
				<ul class="pagination justify-content-center mb-0">
					<li class="page-item" class:disabled={currentPage === 1}>
						<button
							type="button"
							class="page-link"
							aria-label="Pagina precedente"
							onclick={() => goToPage(currentPage - 1)}
						>
							<i class="bi bi-chevron-left"></i>
						</button>
					</li>

					{#each pageNumbers as pageNumber (pageNumber)}
						<li class="page-item" class:active={pageNumber === currentPage}>
							<button type="button" class="page-link" onclick={() => goToPage(pageNumber)}>
								{pageNumber}
							</button>
						</li>
					{/each}

					<li class="page-item" class:disabled={currentPage === totalPages}>
						<button
							type="button"
							class="page-link"
							aria-label="Pagina successiva"
							onclick={() => goToPage(currentPage + 1)}
						>
							<i class="bi bi-chevron-right"></i>
						</button>
					</li>
				</ul>
			</nav>
		{/if}
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
