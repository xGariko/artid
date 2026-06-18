import { writable } from 'svelte/store';
import type { ResolvedPathname } from '$app/types';

export type BreadcrumbCrumb = {
	label: string;
	href: ResolvedPathname;
	icon?: string;     // classe bootstrap-icons (es. "bi-pencil-square")
	iconSrc?: string;  // src di un'icona custom (svg/png)
};

/**
 * Registro dei crumb "extra" che si concatenano DOPO la sezione corrente nella
 * sub-navbar. È una mappa `href -> crumb`: ogni livello di route (pagina O layout)
 * registra SOLO il proprio crumb, e la sub-navbar li compone automaticamente lungo
 * la gerarchia dei path — così si supporta una profondità arbitraria
 * (es. /artid/details/[id]/preview) senza che ogni pagina ripeta gli antenati.
 *
 * `register` ritorna una funzione di cleanup. In Svelte 5 si usa dentro un `$effect`:
 * l'effect richiama il cleanup quando il componente viene distrutto o quando il
 * crumb cambia (es. cambia l'id nel path), togliendo il crumb vecchio e mettendo
 * quello nuovo.
 *
 *   import { breadcrumb } from '$lib/stores/breadcrumb';
 *   import { resolve } from '$app/paths';
 *   import { page } from '$app/state';
 *
 *   $effect(() =>
 *     breadcrumb.register({
 *       label: 'Anteprima',
 *       href: resolve('/(app)/artid/details/[id]/preview', { id: page.params.id }),
 *       icon: 'bi-eye'
 *     })
 *   );
 *
 * NB: gli `$effect` girano solo lato client, quindi i crumb extra compaiono dopo
 * l'hydration (la sezione e la Dashboard sono invece sempre presenti).
 */
const store = writable<Map<string, BreadcrumbCrumb>>(new Map());

export const breadcrumb = {
	subscribe: store.subscribe,
	/** Registra (o aggiorna) un crumb per il suo href. Ritorna la funzione di cleanup. */
	register(crumb: BreadcrumbCrumb): () => void {
		store.update((m) => new Map(m).set(crumb.href, crumb));
		return () => {
			store.update((m) => {
				const next = new Map(m);
				next.delete(crumb.href);
				return next;
			});
		};
	}
};
