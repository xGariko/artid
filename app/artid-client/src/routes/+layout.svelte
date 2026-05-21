<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import '$lib/styles/style.scss';
	import 'bootstrap-icons/font/bootstrap-icons.css';
	import { onMount } from 'svelte';

	let { children } = $props();

	onMount(async () => {
		await import('bootstrap/dist/js/bootstrap.bundle.min.js');
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{@render children()}
