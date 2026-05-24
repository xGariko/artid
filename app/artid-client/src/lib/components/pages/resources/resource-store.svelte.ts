// =============================================================
// Store reattivo della pagina /resources
// =============================================================
// Pattern: factory `createResourcesStore(items)` che incapsula
// $state/$derived (Svelte 5 runes). Espone getter immutabili e
// mutator espliciti — i componenti consumano solo ciò che usano.
//
// Distribuito via Context API: la pagina chiama setResourcesStore(),
// i discendenti useResourcesStore(). Niente prop drilling.
// =============================================================

import { getContext, setContext } from "svelte";
import { SvelteSet } from "svelte/reactivity";
import { getKindMeta, type ResourceCategory, type ResourceCollection, type ResourceFilter, type ResourceListItem } from "./resource-types";

export interface ResourceCounts {
	all: number;
	byCategory: Record<ResourceCategory, number>;
	byCollection: Record<ResourceCollection, number>;
}

export interface ResourcesStore {
	/** Tutti gli item (lettura). */
	readonly items: ResourceListItem[];
	/** Filtro attivo della sidebar. */
	readonly filter: ResourceFilter;
	/** Query di ricerca testuale. */
	readonly search: string;
	/** Id selezionati via checkbox. */
	readonly selected: SvelteSet<number>;
	/** Items filtrati da `filter` + `search`. */
	readonly filtered: ResourceListItem[];
	/** Contatori per ogni voce della sidebar (sempre globali, non filtrati). */
	readonly counts: ResourceCounts;

	setFilter(filter: ResourceFilter): void;
	setSearch(value: string): void;
	toggleSelected(id: number): void;
	clearSelected(): void;
	toggleFavorite(id: number): void;
	isActive(filter: ResourceFilter): boolean;
}

/**
 * Costruisce lo store. Da chiamare una volta nella pagina top-level.
 */
export function createResourcesStore(initial: ResourceListItem[]): ResourcesStore {
	// `items` è $state per supportare toggleFavorite reattivo;
	// SvelteSet è la versione reattiva di Set.
	let items = $state(initial);
	let filter = $state<ResourceFilter>({ kind: "all" });
	let search = $state("");
	const selected = new SvelteSet<number>();

	// Conteggi: derivati da `items` (non da `filtered`!), così la sidebar
	// mostra sempre i totali "veri" anche con filtro attivo.
	const counts = $derived.by<ResourceCounts>(() => {
		const byCategory: Record<ResourceCategory, number> = { document: 0, image: 0, video: 0, audio: 0 };
		let favorite = 0;
		for (const r of items) {
			byCategory[getKindMeta(r.kind).category]++;
			if (r.favorite) favorite++;
		}
		return {
			all: items.length,
			byCategory,
			// Per ora: "recent" = ultimi 30 giorni; "shared" = placeholder
			// (lo daranno gli internal/external share quando wireremo l'API).
			byCollection: {
				recent: countRecent(items),
				favorite,
				shared: Math.min(items.length, 11)
			}
		};
	});

	const filtered = $derived.by<ResourceListItem[]>(() => {
		const term = search.trim().toLowerCase();
		return items.filter((r) => {
			if (!matchesFilter(r, filter)) return false;
			if (term && !r.title.toLowerCase().includes(term)) return false;
			return true;
		});
	});

	return {
		get items() { return items; },
		get filter() { return filter; },
		get search() { return search; },
		get selected() { return selected; },
		get filtered() { return filtered; },
		get counts() { return counts; },

		setFilter(next) {
			filter = next;
			// Cambio filtro = pulisco selezione: gli id selezionati potrebbero
			// non essere più visibili e ammucchierebbero side-effect ambigui.
			selected.clear();
		},
		setSearch(value) {
			search = value;
		},
		toggleSelected(id) {
			if (selected.has(id)) selected.delete(id);
			else selected.add(id);
		},
		clearSelected() {
			selected.clear();
		},
		toggleFavorite(id) {
			items = items.map((r) => (r.id === id ? { ...r, favorite: !r.favorite } : r));
		},
		isActive(candidate) {
			return filtersEqual(filter, candidate);
		}
	};
}

// -------------------------------------------------------------
// Context API — type-safe via Symbol key
// -------------------------------------------------------------

const KEY = Symbol("resources-store");

export function setResourcesStore(store: ResourcesStore): ResourcesStore {
	return setContext(KEY, store);
}

export function useResourcesStore(): ResourcesStore {
	const store = getContext<ResourcesStore | undefined>(KEY);
	if (!store) throw new Error("useResourcesStore() chiamato fuori dal provider della pagina /resources");
	return store;
}

// -------------------------------------------------------------
// Helpers privati
// -------------------------------------------------------------

function matchesFilter(item: ResourceListItem, filter: ResourceFilter): boolean {
	switch (filter.kind) {
		case "all":
			return true;
		case "category":
			return getKindMeta(item.kind).category === filter.category;
		case "collection":
			if (filter.collection === "favorite") return item.favorite;
			if (filter.collection === "recent") return isRecent(item);
			// "shared" non ha ancora dati: placeholder fino al wiring degli share.
			return false;
	}
}

function filtersEqual(a: ResourceFilter, b: ResourceFilter): boolean {
	if (a.kind !== b.kind) return false;
	if (a.kind === "category" && b.kind === "category") return a.category === b.category;
	if (a.kind === "collection" && b.kind === "collection") return a.collection === b.collection;
	return a.kind === "all" && b.kind === "all";
}

const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

function isRecent(item: ResourceListItem): boolean {
	const t = new Date(item.lastModified).getTime();
	return Date.now() - t <= THIRTY_DAYS_MS;
}

function countRecent(items: ResourceListItem[]): number {
	return items.reduce((acc, r) => acc + (isRecent(r) ? 1 : 0), 0);
}
