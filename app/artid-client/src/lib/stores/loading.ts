import { writable } from 'svelte/store';

/**
 * Flag manuale per casi in cui un consumer vuole forzare l'attivazione della
 * progress bar (es. form submit con SvelteKit `use:enhance`).
 */
export const loading = writable<boolean | null>(null);

/**
 * Contatore di richieste API in corso. Incrementato dal middleware su
 * `createApiClient` (ogni chiamata) e da chiunque voglia gestire chiamate
 * fetch raw via `beginRequest`/`endRequest`.
 *
 * La progress bar si attiva automaticamente quando il counter è > 0.
 */
export const activeRequests = writable<number>(0);

export function beginRequest(): void {
	activeRequests.update((n) => n + 1);
}

export function endRequest(): void {
	activeRequests.update((n) => Math.max(0, n - 1));
}
