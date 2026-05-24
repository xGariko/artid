<script lang="ts">
	import type { Pathname } from '$app/types';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';

	let {
		href,
		color = 'primary',
		label
	}: {
		href: Pathname;
		color?: string;
		label: string;
	} = $props();

	const resolved = $derived(resolve(href));

	// Match per PREFISSO: /artid/mozart attiva il link su /artid.
	// Stesso pattern usato dalla sub-navbar (vedi artid-sub-navbar.svelte).
	const isActive = $derived(
		page.url.pathname === resolved || page.url.pathname.startsWith(resolved + '/')
	);
</script>

<a
	href={resolved}
	class="artid-link text-decoration-none text-{color}"
	class:active={isActive}
	aria-current={isActive ? 'page' : undefined}
>
	{label}
</a>

<style>
	.artid-link {
		transition: opacity 0.15s ease;
		opacity: 0.75;
	}

	.artid-link:hover {
		opacity: 1;
	}

	.artid-link.active {
		opacity: 1;
		font-weight: 600;
	}
</style>
