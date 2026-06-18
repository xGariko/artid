// Utilities condivise — formattazione date/dimensioni, classificazione tipi
// di risorsa e mappatura dei badge per estensione file.

export type ResourceType = 'document' | 'image' | 'video' | 'audio';

const ITALIAN_DATE_FORMATTER = new Intl.DateTimeFormat('it-IT', {
	day: 'numeric',
	month: 'short',
	year: 'numeric'
});

// Formatta una data ISO in italiano (es. "7 mag 2026"). Restituisce "—" se assente.
export function formatItalianDate(isoDate: string | null | undefined): string {
	if (!isoDate) return '—';
	return ITALIAN_DATE_FORMATTER.format(new Date(isoDate));
}

const ITALIAN_DATE_FORMATTER_LONG = new Intl.DateTimeFormat('it-IT', {
	day: 'numeric',
	month: 'long',
	year: 'numeric'
});

// Formatta una data ISO in italiano per esteso (es. "12 maggio 2026"). "—" se assente.
export function formatItalianDateLong(isoDate: string | null | undefined): string {
	if (!isoDate) return '—';
	return ITALIAN_DATE_FORMATTER_LONG.format(new Date(isoDate));
}

// Formatta una dimensione in byte come stringa human-readable (kb/mb/gb).
export function formatFileSize(bytes: number | null | undefined): string {
	if (bytes == null) return '—';
	if (bytes < 1024) return `${bytes} b`;
	if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} kb`;
	if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} mb`;
	return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} gb`;
}

// Classifica un MIME type in una macro-categoria usata dai filtri sidebar.
export function resourceTypeFromMime(mimeType: string | null | undefined): ResourceType {
	if (!mimeType) return 'document';
	if (mimeType.startsWith('image/')) return 'image';
	if (mimeType.startsWith('video/')) return 'video';
	if (mimeType.startsWith('audio/')) return 'audio';
	return 'document';
}

// Icona Bootstrap Icons per ciascuna macro-categoria.
export function iconForResourceType(type: ResourceType): string {
	switch (type) {
		case 'image':
			return 'images';
		case 'video':
			return 'film';
		case 'audio':
			return 'music-note-list';
		default:
			return 'file-earmark-richtext';
	}
}

// Palette dei badge per estensione: tinte distinte e leggibili su sfondo chiaro.
const BADGE_COLOR_BY_EXTENSION: Record<string, string> = {
	pdf: '#5DD3D0',
	doc: '#3F8EE6',
	docx: '#3F8EE6',
	txt: '#9AA4AE',
	ppt: '#E55A4C',
	pptx: '#E55A4C',
	xls: '#3FB37A',
	xlsx: '#3FB37A',
	jpg: '#F4B040',
	jpeg: '#F4B040',
	png: '#3FB37A',
	gif: '#F4B040',
	svg: '#F4B040',
	mp4: '#7C5BC3',
	mov: '#7C5BC3',
	avi: '#7C5BC3',
	mp3: '#D85AB0',
	wav: '#D85AB0',
	zip: '#9AA4AE'
};

const DEFAULT_BADGE_COLOR = '#9AA4AE';

// Colore di sfondo del badge tipo file. Fallback su grigio neutro.
export function badgeColorForExtension(extension: string | null | undefined): string {
	if (!extension) return DEFAULT_BADGE_COLOR;
	return BADGE_COLOR_BY_EXTENSION[extension.toLowerCase()] ?? DEFAULT_BADGE_COLOR;
}

// Testo del badge: estensione uppercase, troncata a 4 caratteri.
export function badgeLabelForExtension(extension: string | null | undefined): string {
	return (extension ?? 'FILE').toUpperCase().slice(0, 4);
}

// Soglia (in giorni) entro cui una risorsa è considerata "recente".
export const RECENT_RESOURCES_THRESHOLD_DAYS = 7;

// Timestamp (ms) sotto al quale una risorsa NON è più considerata recente.
export function recentThresholdTimestamp(days: number = RECENT_RESOURCES_THRESHOLD_DAYS): number {
	return Date.now() - days * 24 * 60 * 60 * 1000;
}

// True se `isoDate` ricade nella finestra "recenti".
export function isRecent(isoDate: string | null | undefined, days?: number): boolean {
	if (!isoDate) return false;
	return new Date(isoDate).getTime() >= recentThresholdTimestamp(days);
}

// Iniziali per l'avatar di un profilo (es. "Valeria Seidita" → "VS"). Fallback "?".
export function initialsFor(
	name: string | null | undefined,
	surname: string | null | undefined
): string {
	const firstInitial = (name ?? '').trim().charAt(0);
	const secondInitial = (surname ?? '').trim().charAt(0);
	return `${firstInitial}${secondInitial}`.toUpperCase() || '?';
}

// Palette avatar: tinte distinte e leggibili su testo bianco (in linea con quelle dei badge).
const AVATAR_PALETTE = ['#6f5bd0', '#e08a3c', '#4f9d69', '#3f8ee6', '#d85ab0', '#5dd3d0'];

// Colore avatar deterministico: lo stesso seed (es. nome completo) dà sempre lo stesso colore.
export function avatarColorFor(seed: string | null | undefined): string {
	const text = (seed ?? '').trim();
	let hash = 0;
	for (let index = 0; index < text.length; index++) {
		hash = (hash * 31 + text.charCodeAt(index)) | 0;
	}
	return AVATAR_PALETTE[Math.abs(hash) % AVATAR_PALETTE.length];
}

export type ArtIdFilterType = 'all' | 'mine' | 'sharedWithMe' | 'recent' | 'favourite';
