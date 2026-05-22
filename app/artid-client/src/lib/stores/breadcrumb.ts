import { writable } from 'svelte/store';
import type { ResolvedPathname } from '$app/types';

export type BreadcrumbCrumb = {
	label: string;
	href: ResolvedPathname;
	icon?: string;     // classe bootstrap-icons (es. "bi-pencil-square")
	iconSrc?: string;  // src di un'icona custom (svg/png)
};

/**
 * Crumb aggiuntivi che si concatenano DOPO la sezione corrente nella sub-navbar.
 * Tipicamente settati da una pagina di dettaglio (es. /artid/[id]) per mostrare
 * "Dashboard ── ArtID ── Mozart".
 *
 * Utilizzo da una pagina:
 *
 *   import { onDestroy } from 'svelte';
 *   import { breadcrumb } from '$lib/stores/breadcrumb';
 *
 *   breadcrumb.set([{ label: 'Mozart', href: page.url.pathname, icon: 'bi-pencil-square' }]);
 *   onDestroy(() => breadcrumb.set([]));
 */
export const breadcrumb = writable<BreadcrumbCrumb[]>([]);
