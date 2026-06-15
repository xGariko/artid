<script lang="ts">
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import ExplorePagination from '$lib/components/pages/explore/explore-pagination.svelte';
	import ExploreProfileHeader from '$lib/components/pages/explore/explore-profile-header.svelte';
	import ExplorePublicArtidCard from '$lib/components/pages/explore/explore-public-artid-card.svelte';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();

	const profile = $derived(data.profile);
	const certifications = $derived(profile.certifications ?? []);
	const artids = $derived(profile.artids ?? []);

	const PAGE_SIZE = 6;
	let currentPage = $state(1);

	const totalPages = $derived(Math.max(1, Math.ceil(artids.length / PAGE_SIZE)));
	const pageArtids = $derived(artids.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE));

	function goToPage(targetPage: number): void {
		currentPage = Math.min(Math.max(targetPage, 1), totalPages);
	}
</script>

<ArtidNavbar></ArtidNavbar>

<div
	class="w-100 min-vh-100 d-flex flex-column align-items-center justify-content-start p-3 p-lg-5 explore-detail-container"
>
	<div class="bg-artid-section border border-artid-border rounded-3 overflow-hidden w-100 explore-detail-card">
		<!-- Barra "visualizzazione protetta": la vista pubblica mostra solo contenuti marcati pubblici. -->
		<div class="bg-artid-dark text-white d-flex align-items-center gap-2 px-4 py-2 small">
			<i class="bi bi-person-fill"></i>
			<span>Visualizzazione protetta</span>
			<span class="opacity-50">•</span>
			<span>2026® ArtID</span>
		</div>

		<div class="p-4">
			<ExploreProfileHeader {profile} />

			{#if certifications.length > 0}
				<div class="d-flex flex-wrap gap-2 mt-4">
					{#each certifications as certification (certification.id)}
						<span
							class="border border-artid-border rounded-2 px-3 py-2 d-flex align-items-center gap-2 text-artid-text small"
						>
							<i class="bi bi-file-earmark-text text-primary"></i>
							{certification.title}
						</span>
					{/each}
				</div>
			{/if}

			<h2 class="font-lora fw-bold text-artid-text mt-4 mb-3 fs-4">ArtID pubblici</h2>

			{#if artids.length > 0}
				<div class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 g-3">
					{#each pageArtids as artid (artid.id)}
						<div class="col">
							<ExplorePublicArtidCard {artid} />
						</div>
					{/each}
				</div>

				<ExplorePagination {currentPage} {totalPages} onChange={goToPage} class="mt-4" />
			{:else}
				<div class="text-artid-text-muted text-center py-5">
					<i class="bi bi-collection fs-1 d-block mb-2"></i>
					Nessun ArtID pubblico
				</div>
			{/if}
		</div>
	</div>
</div>

<style>
	.explore-detail-container {
		padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
		background-color: var(--artid-surface);
	}

	/* Larghezza leggibile del riquadro, centrato: nessuna utility Bootstrap per un max-width in px. */
	.explore-detail-card {
		max-width: 960px;
	}
</style>
