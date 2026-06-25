<script lang="ts">
	import { breadcrumb } from '$lib/stores/breadcrumb';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import PublicArtidDetailView from '$lib/components/pages/explore/public-artid-detail-view.svelte';
	import type { PageData } from './$types';

	// `data.detail` ← +page.server.ts (read-model condiviso con Explore). La vista è identica a quella
	// pubblica: stesso componente PublicArtidDetailView.
	let { data }: { data: PageData } = $props();

	$effect(() =>
		breadcrumb.register({
			label: 'Anteprima',
			href: resolve('/(app)/artid/details/[id]/preview', { id: page.params.id! }),
			icon: 'bi-eye'
		})
	);
</script>

<div class="p-3 p-lg-4">
	<PublicArtidDetailView artid={data.detail} />
</div>
