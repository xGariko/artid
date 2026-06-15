// Sanitizzazione HTML centralizzata: UNICO punto in cui si definisce cosa è ammesso nel
// markup renderizzato con {@html}. Qualunque rich-text che finisce in {@html ...} DEVE passare
// da qui — è la difesa contro XSS stored (es. la biografia di un altro utente mostrata nel
// profilo pubblico, o una descrizione di ArtID/risorsa).
//
// L'allowlist è tarata sull'output degli editor Quill dell'app (biografia profilo, descrizione
// risorse e ArtID condividono la stessa toolbar), ma resta valida per qualsiasi HTML: tutto ciò
// che non è elencato (<script>, <iframe>, attributi on*, URI javascript:/data:) viene rimosso.
import DOMPurify from 'isomorphic-dompurify';

const SANITIZE_OPTIONS = {
	ALLOWED_TAGS: [
		'p',
		'br',
		'span',
		'strong',
		'em',
		'u',
		's',
		'blockquote',
		'ol',
		'ul',
		'li',
		'a',
		'h1',
		'h2',
		'h3',
		'h4',
		'h5',
		'h6',
		'pre',
		'code',
		'sub',
		'sup'
	],
	ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'style'],
	ALLOWED_URI_REGEXP: /^(?:https?|mailto):/i
};

// Hook globale sul singleton DOMPurify: registrato una sola volta all'import del modulo.
// I link esterni aperti in nuova scheda non devono poter manipolare la tab d'origine.
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
	if (node.tagName === 'A' && node.hasAttribute('href')) {
		node.setAttribute('target', '_blank');
		node.setAttribute('rel', 'noopener noreferrer');
	}
});

/**
 * Ripulisce una stringa HTML rendendola sicura per il rendering con {@html}.
 * Restituisce stringa vuota se l'input è assente.
 */
export function sanitizeHtml(html?: string | null): string {
	return html ? DOMPurify.sanitize(html, SANITIZE_OPTIONS) : '';
}
