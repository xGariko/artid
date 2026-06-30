<script lang="ts">
	import type { PublicArtidSummary } from '$lib/api/types';
	import { formatItalianDateLong } from '$lib/utilities';
	import { resolve } from '$app/paths';
	import artidPlaceholder from '$lib/assets/artid_logo_outline_primary.svg';

	let { artid, userId }: { artid: PublicArtidSummary; userId: number } = $props();

	const resourceCount = $derived(artid.resourceCount ?? 0);
	// thumbnailUrl è un presigned URL: può essere null (nessuna copertina) o fallire il caricamento
	// (URL scaduto/404). In entrambi i casi ripieghiamo sul logo come segnaposto.
	let thumbnailFailed = $state(false);
	const hasThumbnail = $derived(!!artid.thumbnailUrl && !thumbnailFailed);
	const thumbnailSrc = $derived(hasThumbnail ? artid.thumbnailUrl! : artidPlaceholder);
	// Dettaglio ArtID pubblico: /explore/{userId}/artid/{artidId}.
	const detailHref = $derived(
		resolve('/explore/[id]/artid/[artidId]', { id: String(userId), artidId: String(artid.id) })
	);
</script>

<a
	href={detailHref}
	class="bg-artid-section border border-artid-border rounded-3 p-3 h-100 d-flex flex-column text-decoration-none public-artid-card"
>
	<div class="d-flex gap-3 align-items-center">
		<div class="flex-shrink-0">
			<img
				src={thumbnailSrc}
				alt={artid.title}
				class="rounded-2 bg-artid-surface public-artid-card__thumb"
				class:public-artid-card__thumb--placeholder={!hasThumbnail}
				onerror={() => (thumbnailFailed = true)}
			/>
		</div>
		<div class="d-flex flex-column overflow-hidden">
			<div class="font-lora fw-bold fs-5 text-artid-text text-truncate" title={artid.title}>
				{artid.title}
			</div>
			<div class="text-muted small mt-1">

				Creato il {formatItalianDateLong(artid.createdAt)}
			</div>
		</div>
	</div>

	<div class="mt-auto pt-3 text-muted small">
		Contiene {resourceCount}
		{resourceCount === 1 ? 'risorsa' : 'risorse'}
	</div>
</a>

<style>
    /* Hover discreto: il bordo si tinge di brand. Nessuna utility Bootstrap per il bordo in hover. */
    .public-artid-card {
        transition: border-color 0.15s ease;
        cursor: pointer;
    }

    .public-artid-card:hover {
        border-color: var(--artid-primary) !important;
				background: var(--artid-primary-subtle) !important;
    }

    /* Thumbnail quadrata: il presigned URL può essere una foto qualsiasi → cover per non deformarla. */
    .public-artid-card__thumb {
        width: 3.5rem;
        height: 3.5rem;
        object-fit: cover;
    }

    /* Segnaposto (nessuna copertina o URL non valido): logo contenuto, non ritagliato come una foto. */
    .public-artid-card__thumb--placeholder {
        object-fit: contain;
        padding: 0.4rem;
    }
</style>
