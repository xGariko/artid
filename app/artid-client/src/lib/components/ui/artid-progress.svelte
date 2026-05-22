<script lang="ts">
	import { loading, activeRequests } from '$lib/stores/spinner-loading';
	import { navigating, page } from '$app/state';
	import { resolve } from '$app/paths';

	// Attiva la barra se uno qualsiasi dei segnali è attivo:
	// - flag manuale (`loading`) per casi forzati (form submit ecc.)
	// - counter di richieste API in corso (middleware su createApiClient)
	// - navigazione SvelteKit in corso (load/+page.server.ts in esecuzione)
	let active = $derived(Boolean($loading) || $activeRequests > 0 || navigating.to !== null);

	// Posizionamento dinamico: la sub-navbar è nascosta sulla dashboard
	// (vedi artid-sub-navbar.svelte). In quel caso attacchiamo la barra
	// direttamente sotto la navbar; altrove la mettiamo sotto la sub-navbar.
	// Usiamo SEMPRE `page.url.pathname` (path corrente) e non
	// `navigating.to`, perché la sub-navbar visibile è quella della pagina
	// ancora montata — quindi la barra deve allinearsi a lei finché la
	// navigazione non si conclude.
	const DASHBOARD = resolve('/dashboard');
	let onDashboard = $derived(page.url.pathname === DASHBOARD);
	let topStyle = $derived(
		onDashboard
			? 'var(--artid-navbar-height, 0px)'
			: 'calc(var(--artid-navbar-height, 0px) + var(--artid-navbar-height, 0px) / 2)'
	);
</script>

{#if active}
	<div
		class="artid-progress"
		role="progressbar"
		aria-busy="true"
		aria-label="Caricamento in corso"
		style="top: {topStyle};"
	>
		<div class="artid-progress__bar"></div>
	</div>
{/if}

<style>
	.artid-progress {
		position: fixed;
		left: 0;
		right: 0;
		height: 5px;
		background: transparent;
		overflow: hidden;
		z-index: 2000;
		pointer-events: none;
		transition: top 180ms ease;
	}

	.artid-progress__bar {
		position: absolute;
		top: 0;
		left: 0;
		height: 100%;
		width: 40%;
		background-color: var(--artid-primary);
		border-radius: 0;
		will-change: transform;
		animation: artid-progress-slide 1.4s cubic-bezier(0.4, 0, 0.2, 1) infinite;
	}

	@keyframes artid-progress-slide {
		0% {
			transform: translateX(-100%);
		}
		100% {
			transform: translateX(250%);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.artid-progress__bar {
			animation-duration: 2.4s;
		}
	}
</style>
