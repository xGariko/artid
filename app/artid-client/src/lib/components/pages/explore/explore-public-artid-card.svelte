<script lang="ts">
	import type { PublicArtidSummary } from '$lib/api/types';
	import { formatItalianDateLong } from '$lib/utilities';

	let { artid }: { artid: PublicArtidSummary } = $props();

	const resourceCount = $derived(artid.resourceCount ?? 0);
</script>

<div
	class="bg-artid-section border border-artid-border rounded-3 p-3 h-100 d-flex flex-column public-artid-card"
>
	<div class="font-lora fw-bold fs-5 text-artid-text text-truncate" title={artid.title}>
		{artid.title}
	</div>

	<div class="text-muted small mt-1">
		Creato il {formatItalianDateLong(artid.createdAt)}
	</div>

	<div class="mt-auto pt-3 text-muted small">
		Contiene {resourceCount}
		{resourceCount === 1 ? 'risorsa' : 'risorse'}
	</div>
</div>

<style>
	/* Hover discreto: il bordo si tinge di brand. Nessuna utility Bootstrap per il bordo in hover. */
	.public-artid-card {
		transition: border-color 0.15s ease;
	}

	.public-artid-card:hover {
		border-color: var(--artid-primary) !important;
	}
</style>
