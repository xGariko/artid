<script lang="ts">
	import { breadcrumb } from '$lib/stores/breadcrumb';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';

	// `data` arriva dal +layout.server.ts di questo livello (carica l'ArtID dell'utente).
	let { data, children } = $props();

	$effect(() =>
		breadcrumb.register({
			label: data.artid.title ?? 'ArtID',
			href: resolve('/(app)/artid/details/[id]', { id: page.params.id! }),
			icon: 'bi-pencil-square'
		})
	);
</script>

{@render children()}
