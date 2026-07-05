<script lang="ts">
	import type { PublicArtidDetail } from '$lib/api/types';
	import PublicMaterialCard from '$lib/components/pages/explore/public-material-card.svelte';
	import ContactEmailButton from '$lib/components/pages/explore/contact-email-button.svelte';
	import { sanitizeHtml } from '$lib/sanitize';
	import { avatarColorFor, formatItalianDateLong, initialsFor, resourceTypeFromMime, socialUrlFor } from '$lib/utilities';
	import { resolve } from '$app/paths';
	import artidPlaceholder from '$lib/assets/artid_logo_outline_primary.svg';

	// Vista condivisa del dettaglio ArtID "come appare pubblicamente": usata sia dalla pagina Explore
	// pubblica sia dall'anteprima del proprietario, così i due contesti restano identici.
	let { artid }: { artid: PublicArtidDetail } = $props();

	const materials = $derived(artid.materials ?? []);

	const authorFullName = $derived(`${artid.authorName ?? ''} ${artid.authorSurname ?? ''}`.trim());
	const authorInitials = $derived(initialsFor(artid.authorName, artid.authorSurname));
	const authorColor = $derived(avatarColorFor(authorFullName));
	// Senza thumbnail (o con URL presigned scaduto/404) mostriamo il logo come segnaposto:
	// va "contenuto" (non ritagliato come una foto).
	let thumbnailFailed = $state(false);
	const hasThumbnail = $derived(!!artid.thumbnailUrl && !thumbnailFailed);
	const thumbnailSrc = $derived(hasThumbnail ? artid.thumbnailUrl! : artidPlaceholder);

	const profileHref = $derived(resolve('/explore/[id]', { id: String(artid.authorId) }));
	// Profilo autore privato (contesti di condivisione): la pagina profilo risponderebbe 404,
	// quindi disabilitiamo "Vai al profilo". Assente/undefined = comportamento storico (abilitato).
	const profilePublic = $derived(artid.authorProfilePublic ?? true);

	// Handle/URL social dell'autore normalizzati verso l'URL completo del profilo (null = niente bottone).
	const linkedinUrl = $derived(socialUrlFor('linkedin', artid.authorLinkedinId));
	const facebookUrl = $derived(socialUrlFor('facebook', artid.authorFacebookId));
	const instagramUrl = $derived(socialUrlFor('instagram', artid.authorInstagramId));

	// Larghezza colonna per tipo (come nel mockup): video a tutta larghezza, immagini 2-up, audio/file 3-up.
	function materialColClass(mimeType: string | null | undefined): string {
		const type = resourceTypeFromMime(mimeType);
		if (type === 'video') return 'col-12';
		if (type === 'image') return 'col-12 col-md-6';
		return 'col-12 col-md-6 col-lg-4';
	}
</script>

<div
	class="bg-artid-section border border-artid-border rounded-3 overflow-hidden w-100 artid-detail-card mx-auto"
>
	<!-- Barra "visualizzazione protetta": vista pubblica, solo contenuti marcati pubblici. -->
	<div class="bg-artid-dark text-white d-flex align-items-center gap-2 px-4 py-2 small">
		<i class="bi bi-person-fill"></i>
		<span>Visualizzazione protetta</span>
		<span class="opacity-50">•</span>
		<span>2026® ArtID</span>
	</div>

	<div class="p-4">
		<!-- Header: autore + titolo ArtID + azioni -->
		<div class="d-flex align-items-start justify-content-between gap-3 flex-wrap">
			<div class="d-flex align-items-center gap-3">
				{#if artid.authorAvatarUrl}
					<img
						src={artid.authorAvatarUrl}
						alt={authorFullName}
						class="rounded-circle artid-detail__avatar"
					/>
				{:else}
					<div
						class="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold fs-4 artid-detail__avatar"
						style:background-color={authorColor}
						aria-hidden="true"
					>
						{authorInitials}
					</div>
				{/if}

				<div>
					<h1 class="font-lora fw-bold text-artid-text mb-1 fs-3">{artid.title}</h1>
					<div class="text-artid-text-muted small">
						di <span class="fw-semibold">{authorFullName}</span>{#if artid.authorProfession}
							- {artid.authorProfession}{/if}
					</div>
					<div class="text-artid-text-muted small">
						Creato il {formatItalianDateLong(artid.createdAt)}
					</div>
				</div>
			</div>

			<div class="d-flex align-items-center gap-2">
				<a
					href={profilePublic ? profileHref : undefined}
					class="btn btn-primary rounded-2 px-3 py-2 fw-semibold"
					class:disabled={!profilePublic}
					aria-disabled={!profilePublic}
					tabindex={profilePublic ? undefined : -1}
				>
					Vai al profilo
				</a>
				{#if linkedinUrl}
					<a
						href={linkedinUrl}
						target="_blank"
						rel="noopener"
						class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center artid-detail__icon-btn"
						aria-label="Profilo LinkedIn"
					>
						<i class="bi bi-linkedin"></i>
					</a>
				{/if}
				{#if facebookUrl}
					<a
						href={facebookUrl}
						target="_blank"
						rel="noopener"
						class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center artid-detail__icon-btn"
						aria-label="Profilo Facebook"
					>
						<i class="bi bi-facebook"></i>
					</a>
				{/if}
				{#if instagramUrl}
					<a
						href={instagramUrl}
						target="_blank"
						rel="noopener"
						class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center artid-detail__icon-btn"
						aria-label="Profilo Instagram"
					>
						<i class="bi bi-instagram"></i>
					</a>
				{/if}
				{#if artid.authorBusinessEmail}
					<ContactEmailButton email={artid.authorBusinessEmail} />
				{/if}
			</div>
		</div>

		<!-- Thumbnail + descrizione del progetto -->
		<div class="row g-4 mt-1">
			<div class="col-12 col-lg-5">
				<img
					src={thumbnailSrc}
					alt={artid.title}
					class="w-100 rounded-3 border border-artid-border artid-detail__thumb"
					class:artid-detail__thumb--placeholder={!hasThumbnail}
					onerror={() => (thumbnailFailed = true)}
				/>
			</div>
			<div class="col-12 col-lg-7">
				<h2 class="fw-bold text-artid-text fs-5 mb-2">Descrizione</h2>
				{#if artid.description}
					<!-- Descrizione rich-text (Quill): sanitizzata prima del rendering con {@html} (vedi $lib/sanitize). -->
					<div class="text-artid-text mb-0 artid-detail__description">
						{@html sanitizeHtml(artid.description)}
					</div>
				{:else}
					<p class="text-artid-text-muted mb-0">Nessuna descrizione disponibile.</p>
				{/if}
			</div>
		</div>

		<!-- Materiali associati: i media si caricano solo al click (vedi PublicMaterialCard). -->
		<h2 class="fw-bold text-artid-text fs-5 mt-5 mb-3">Materiali associati</h2>
		{#if materials.length > 0}
			<div class="row g-3">
				{#each materials as material (material.id)}
					<div class={materialColClass(material.mimeType)}>
						<PublicMaterialCard {material} />
					</div>
				{/each}
			</div>
		{:else}
			<div class="text-artid-text-muted text-center py-5 border border-artid-border rounded-3">
				<i class="bi bi-collection fs-1 d-block mb-2"></i>
				Nessun materiale associato
			</div>
		{/if}
	</div>

	<!-- Footer -->
	<div
		class="bg-artid-dark text-white d-flex align-items-center justify-content-center gap-2 px-4 py-2 small"
	>
		<span>© 2026 ArtID</span>
	</div>
</div>

<style>
	/* Larghezza leggibile del riquadro, centrato (come il dettaglio profilo). */
	.artid-detail-card {
		max-width: 960px;
	}

	.artid-detail__avatar {
		width: 4.5rem;
		height: 4.5rem;
		flex-shrink: 0;
		object-fit: cover;
	}

	.artid-detail__icon-btn {
		width: 2.75rem;
		height: 2.75rem;
	}

	.artid-detail__thumb {
		aspect-ratio: 4 / 3;
		object-fit: cover;
	}

	/* Segnaposto (ArtID senza copertina): logo centrato e contenuto, non ritagliato come una foto. */
	.artid-detail__thumb--placeholder {
		object-fit: contain;
		padding: 2.5rem;
		background-color: var(--artid-surface);
	}

	/* La descrizione è rich-text (Quill): azzeriamo i margini di primo/ultimo blocco così resta
	   allineata al titolo "Descrizione" e mantiene il comportamento del vecchio mb-0. */
	.artid-detail__description :global(> :first-child) {
		margin-top: 0;
	}

	.artid-detail__description :global(> :last-child) {
		margin-bottom: 0;
	}
</style>
