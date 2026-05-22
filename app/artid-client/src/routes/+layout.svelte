<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import '$lib/styles/style.scss';
	import 'bootstrap-icons/font/bootstrap-icons.css';
	import { onMount } from 'svelte';
	import { user } from '$lib/stores/auth';
	import ArtidProgress from '$lib/components/ui/artid-progress.svelte';
	import type { LayoutData } from './$types';

	let { children, data }: { children: import('svelte').Snippet; data: LayoutData } = $props();

	user.set(data.user);

	$effect.pre(() => {
		user.set(data.user);
	});

	onMount(async () => {
		await import('bootstrap/dist/js/bootstrap.bundle.min.js');
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

<ArtidProgress />

{@render children()}
