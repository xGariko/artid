<script lang="ts">
	import PublicArtidDetailView from '$lib/components/pages/explore/public-artid-detail-view.svelte';
	import { breadcrumb } from '$lib/stores/breadcrumb';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { PageData } from './$types';

	// `data.detail` ← +page.server.ts. È lo stesso read-model pubblico di Explore/anteprima
	// (PublicArtidDetailView), ma servito tramite la condivisione interna: così è visibile anche se
	// l'ArtID non è pubblico. Restiamo nel layout (app) per conservare header + subnavbar.
	let { data }: { data: PageData } = $props();

	$effect(() =>
		breadcrumb.register({
			label: 'Condiviso con me',
			href: resolve('/(app)/artid/shared/[artidId]', { artidId: page.params.artidId! }),
			icon: 'bi-share'
		})
	);
</script>

<div class="d-flex flex-column align-items-center">
	<PublicArtidDetailView artid={data.detail} />
</div>
