<script lang="ts">
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import PublicArtidDetailView from '$lib/components/pages/explore/public-artid-detail-view.svelte';
	import { breadcrumb } from '$lib/stores/breadcrumb';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { PageData } from './$types';

	// `data.detail` ← +page.server.ts (read-model condiviso con Explore). Il dettaglio è la stessa vista
	// pubblica (PublicArtidDetailView); restiamo nel layout (app) per conservare nav + subnavbar.
	let { data }: { data: PageData } = $props();

	$effect(() =>
		breadcrumb.register({
			label: 'Anteprima',
			href: resolve('/(app)/artid/details/[id]/preview', { id: page.params.id! }),
			icon: 'bi-eye'
		})
	);

	function backToEdit(): void {
		goto(resolve('/(app)/artid/details/[id]', { id: page.params.id! }));
	}
</script>

<div class="d-flex flex-column align-items-center">
	<!-- Barra "Anteprima": unico elemento in più rispetto alla vista pubblica, segnala il contesto e
	     offre il ritorno alla modifica. Allineata alla card (stesso max-width). -->
	<div class="preview-banner-width w-100 mx-auto mb-3">
		<div
			class="bg-artid-primary-subtle border border-artid-border rounded-3 px-4 py-2 d-flex align-items-center justify-content-between gap-3"
		>
			<span class="d-flex align-items-center gap-2 text-artid-text fw-semibold small">
				<i class="bi bi-eye"></i>
				Anteprima — così appare il tuo ArtID pubblicamente
			</span>
			<ArtidButton
				label="Torna alla modifica"
				icon="pencil-square"
				fullWidth={false}
				btnStyle="secondary"
				outline={true}
				onclick={backToEdit}
			/>
		</div>
	</div>

	<PublicArtidDetailView artid={data.detail} />
</div>

<style>
	/* Allinea la barra "Anteprima" alla larghezza della card del dettaglio. */
	.preview-banner-width {
		max-width: 960px;
	}
</style>
