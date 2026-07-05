<script lang="ts">
	import ArtidNavbar from '$lib/components/layout/artid-navbar.svelte';
	import { page } from '$app/state';

	// Opzionale: puoi renderlo dinamico.
	// In SvelteKit l'errore di rete spesso genera uno status 0 o 500
	let isOfflineError = $derived(page.status === 0 || page.status === 500 || page.error?.message?.includes('fetch'));
</script>

<!-- Passiamo la prop per forzare il pulsante "Accedi" -->
<ArtidNavbar forceLoggedOut={true} />

<div
	class="w-100 min-vh-100 d-flex flex-column align-items-center justify-content-center p-4 text-center error-container"
>
	<i class="bi bi-link-45deg text-artid-text-muted error-icon"></i>
	<h1 class="h4 fw-semibold text-artid-text mt-3">Link non disponibile</h1>
	<p class="text-artid-text-muted mb-0">{page.error?.message}</p>
</div>

<style>
    .error-container {
        padding-top: calc(var(--artid-navbar-height) + 1.5rem) !important;
        background-color: var(--artid-surface);
    }

    .error-icon {
        font-size: 3rem;
    }
</style>