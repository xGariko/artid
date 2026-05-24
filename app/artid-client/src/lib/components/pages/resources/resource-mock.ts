// =============================================================
// Mock data per la pagina /resources
// =============================================================
// Sostituibile da una fetch reale: la shape è già quella consumata
// dalla UI (ResourceListItem). Quando arriverà l'API basterà
// rimpiazzare l'import nel +page.svelte (o introdurre un loader
// in +page.server.ts) — nessun componente cambia.
// =============================================================

import type { ResourceListItem } from "./resource-types";

const KB = 1024;
const MB = KB * 1024;

export const MOCK_RESOURCES: ResourceListItem[] = [
	{ id: 1,  title: "Risorsa 1",  kind: "pdf",  sizeBytes: 781 * KB,  createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 5, favorite: false },
	{ id: 2,  title: "Risorsa 2",  kind: "jpg",  sizeBytes: 3.4 * MB,  createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 3, favorite: true  },
	{ id: 3,  title: "Risorsa 3",  kind: "ppt",  sizeBytes: 781 * KB,  createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 1, favorite: true  },
	{ id: 4,  title: "Risorsa 4",  kind: "mp4",  sizeBytes: 75.2 * MB, createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 0, favorite: false },
	{ id: 5,  title: "Risorsa 5",  kind: "pdf",  sizeBytes: 781 * KB,  createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 3, favorite: false },
	{ id: 6,  title: "Risorsa 6",  kind: "pdf",  sizeBytes: 781 * KB,  createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 2, favorite: false },
	{ id: 7,  title: "Risorsa 7",  kind: "pdf",  sizeBytes: 781 * KB,  createdAt: "2026-05-07", lastModified: "2026-05-07", linkedArtidCount: 2, favorite: true  },
	{ id: 8,  title: "Catalogo mostra",       kind: "pdf", sizeBytes: 2.1 * MB,  createdAt: "2026-04-22", lastModified: "2026-05-02", linkedArtidCount: 7, favorite: false },
	{ id: 9,  title: "Vista frontale",        kind: "jpg", sizeBytes: 1.8 * MB,  createdAt: "2026-04-18", lastModified: "2026-04-18", linkedArtidCount: 1, favorite: false },
	{ id: 10, title: "Vista posteriore",      kind: "png", sizeBytes: 2.4 * MB,  createdAt: "2026-04-18", lastModified: "2026-04-18", linkedArtidCount: 1, favorite: false },
	{ id: 11, title: "Presentazione 2026",    kind: "ppt", sizeBytes: 8.7 * MB,  createdAt: "2026-04-15", lastModified: "2026-05-01", linkedArtidCount: 4, favorite: true  },
	{ id: 12, title: "Spot promozionale",     kind: "mp4", sizeBytes: 124 * MB,  createdAt: "2026-04-10", lastModified: "2026-04-10", linkedArtidCount: 2, favorite: false },
	{ id: 13, title: "Intervista curatore",   kind: "mov", sizeBytes: 312 * MB,  createdAt: "2026-04-05", lastModified: "2026-04-05", linkedArtidCount: 0, favorite: false },
	{ id: 14, title: "Audioguida sala 1",     kind: "mp3", sizeBytes: 4.6 * MB,  createdAt: "2026-03-28", lastModified: "2026-04-12", linkedArtidCount: 6, favorite: true  },
	{ id: 15, title: "Audioguida sala 2",     kind: "mp3", sizeBytes: 5.1 * MB,  createdAt: "2026-03-28", lastModified: "2026-03-28", linkedArtidCount: 6, favorite: false },
	{ id: 16, title: "Registrazione live",    kind: "wav", sizeBytes: 48 * MB,   createdAt: "2026-03-20", lastModified: "2026-03-20", linkedArtidCount: 0, favorite: false },
	{ id: 17, title: "Scheda tecnica",        kind: "doc", sizeBytes: 220 * KB,  createdAt: "2026-03-15", lastModified: "2026-04-30", linkedArtidCount: 9, favorite: true  },
	{ id: 18, title: "Inventario opere",      kind: "xls", sizeBytes: 540 * KB,  createdAt: "2026-03-10", lastModified: "2026-05-10", linkedArtidCount: 12, favorite: false },
	{ id: 19, title: "Note conservatore",     kind: "txt", sizeBytes: 12 * KB,   createdAt: "2026-03-05", lastModified: "2026-03-05", linkedArtidCount: 0, favorite: false },
	{ id: 20, title: "Logo istituzionale",    kind: "png", sizeBytes: 340 * KB,  createdAt: "2026-02-28", lastModified: "2026-02-28", linkedArtidCount: 15, favorite: true },
	{ id: 21, title: "GIF promozionale",      kind: "gif", sizeBytes: 980 * KB,  createdAt: "2026-02-20", lastModified: "2026-02-20", linkedArtidCount: 2, favorite: false },
	{ id: 22, title: "Catalogo 2025",         kind: "pdf", sizeBytes: 14 * MB,   createdAt: "2026-02-10", lastModified: "2026-02-10", linkedArtidCount: 3, favorite: false },
	{ id: 23, title: "Schede sala A",         kind: "pdf", sizeBytes: 1.2 * MB,  createdAt: "2026-01-30", lastModified: "2026-03-12", linkedArtidCount: 8, favorite: false }
];
