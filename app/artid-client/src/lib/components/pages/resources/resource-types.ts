// =============================================================
// Resources page — view-model + tassonomia tipi
// =============================================================
// Mantengo lo schema persistente (Resource + File) separato dal view-model
// che la UI consuma: lì compaiono campi derivati (kind, sizeBytes,
// linkedArtidCount) che semplificano table e badge senza forzare la
// denormalizzazione nel modello dati.
// =============================================================

/**
 * Categorie di alto livello mostrate nella sidebar "Tipo".
 * Ogni `ResourceKind` mappa esattamente una categoria.
 */
export type ResourceCategory = "document" | "image" | "video" | "audio";

/**
 * Tipo di file riconosciuto. Estendere qui (e in RESOURCE_KIND_META)
 * quando arrivano nuovi formati. Il fallback "other" copre l'ignoto.
 */
export type ResourceKind =
	| "pdf"
	| "doc"
	| "xls"
	| "ppt"
	| "txt"
	| "jpg"
	| "png"
	| "gif"
	| "mp4"
	| "mov"
	| "mp3"
	| "wav"
	| "other";

/**
 * Metadati di un kind — colore badge, categoria, label visualizzata.
 * Le classi sono utility Bootstrap del tema custom (vedi style.scss).
 */
export interface ResourceKindMeta {
	label: string;
	category: ResourceCategory;
	/** Classe background per il badge (Bootstrap utility). */
	bgClass: string;
	/** Classe testo che garantisce contrasto sul bgClass. */
	textClass: string;
}

export const RESOURCE_KIND_META: Record<ResourceKind, ResourceKindMeta> = {
	pdf:   { label: "PDF",   category: "document", bgClass: "bg-info-subtle",      textClass: "text-info-emphasis" },
	doc:   { label: "DOC",   category: "document", bgClass: "bg-primary-subtle",   textClass: "text-primary-emphasis" },
	xls:   { label: "XLS",   category: "document", bgClass: "bg-success-subtle",   textClass: "text-success-emphasis" },
	ppt:   { label: "PPTX",  category: "document", bgClass: "bg-warning-subtle",   textClass: "text-warning-emphasis" },
	txt:   { label: "TXT",   category: "document", bgClass: "bg-secondary-subtle", textClass: "text-secondary-emphasis" },
	jpg:   { label: "JPG",   category: "image",    bgClass: "bg-warning-subtle",   textClass: "text-warning-emphasis" },
	png:   { label: "PNG",   category: "image",    bgClass: "bg-info-subtle",      textClass: "text-info-emphasis" },
	gif:   { label: "GIF",   category: "image",    bgClass: "bg-danger-subtle",    textClass: "text-danger-emphasis" },
	mp4:   { label: "MP4",   category: "video",    bgClass: "bg-primary-subtle",   textClass: "text-primary-emphasis" },
	mov:   { label: "MOV",   category: "video",    bgClass: "bg-info-subtle",      textClass: "text-info-emphasis" },
	mp3:   { label: "MP3",   category: "audio",    bgClass: "bg-success-subtle",   textClass: "text-success-emphasis" },
	wav:   { label: "WAV",   category: "audio",    bgClass: "bg-warning-subtle",   textClass: "text-warning-emphasis" },
	other: { label: "FILE",  category: "document", bgClass: "bg-secondary-subtle", textClass: "text-secondary-emphasis" }
};

/**
 * View-model consumato da tabella/sidebar. È volutamente piatto:
 * la pagina non deve sapere di File/Resource/ArtidResource, e quando
 * arriverà l'API basterà una funzione di mapping verso questa shape.
 */
export interface ResourceListItem {
	id: number;
	title: string;
	kind: ResourceKind;
	sizeBytes: number;
	createdAt: string; // ISO 8601
	lastModified: string; // ISO 8601
	linkedArtidCount: number;
	favorite: boolean;
}

/**
 * Filtro attivo della sidebar. "all" = nessun filtro per categoria.
 * Le collection (recents/favorites/shared) sono filtri trasversali
 * indipendenti dalla categoria.
 */
export type ResourceFilter =
	| { kind: "all" }
	| { kind: "category"; category: ResourceCategory }
	| { kind: "collection"; collection: ResourceCollection };

export type ResourceCollection = "recent" | "favorite" | "shared";

// -------------------------------------------------------------
// Helpers puri — facili da testare, niente dipendenze su Svelte.
// -------------------------------------------------------------

/**
 * Converte byte in stringa human-readable stile "3.4 mb" / "781 kb".
 * Volutamente minuscolo per matchare il design dello screenshot.
 */
export function formatBytes(bytes: number): string {
	if (bytes < 1024) return `${bytes} b`;
	const kb = bytes / 1024;
	if (kb < 1024) return `${kb.toFixed(kb < 10 ? 1 : 0)} kb`;
	const mb = kb / 1024;
	if (mb < 1024) return `${mb.toFixed(mb < 10 ? 1 : 0)} mb`;
	const gb = mb / 1024;
	return `${gb.toFixed(1)} gb`;
}

/**
 * Formatta una data ISO in "7 Mag 2026" (italiano abbreviato).
 */
const MONTHS_IT = ["Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic"];

export function formatDateIt(iso: string): string {
	const d = new Date(iso);
	return `${d.getDate()} ${MONTHS_IT[d.getMonth()]} ${d.getFullYear()}`;
}

/**
 * Restituisce i meta del kind, con fallback su "other" se sconosciuto.
 */
export function getKindMeta(kind: ResourceKind): ResourceKindMeta {
	return RESOURCE_KIND_META[kind] ?? RESOURCE_KIND_META.other;
}
