// Sanitizzazione HTML centralizzata: UNICO punto in cui si definisce cosa è ammesso nel
// markup renderizzato con {@html}. Qualunque rich-text che finisce in {@html ...} DEVE passare
// da qui — è la difesa contro XSS stored (es. la biografia di un altro utente mostrata nel
// profilo pubblico, o una descrizione di ArtID/risorsa).
//
// L'allowlist è tarata sull'output degli editor Quill dell'app (biografia profilo, descrizione
// risorse e ArtID condividono la stessa toolbar), ma resta valida per qualsiasi HTML: tutto ciò
// che non è elencato (<script>, <iframe>, attributi on*, URI javascript:/data:) viene rimosso.
//
// Usiamo `sanitize-html` (parser puro-JS) e NON DOMPurify: DOMPurify richiede un DOM, quindi in SSR
// dipende da jsdom (isomorphic-dompurify). Su Vercel serverless quel bundle si rompe e il render SSR
// della pagina va in 500; questo parser invece gira identico in Node/serverless e nel browser.
import sanitizeHtmlLib from 'sanitize-html';

const ALLOWED_TAGS = [
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
];

const SANITIZE_OPTIONS: sanitizeHtmlLib.IOptions = {
	allowedTags: ALLOWED_TAGS,
	// href/target/rel solo sui link; class/style ovunque (le classi/allineamenti prodotti da Quill).
	allowedAttributes: {
		a: ['href', 'target', 'rel'],
		'*': ['class', 'style']
	},
	// Solo http/https/mailto: niente javascript:, data:, ftp: o URL protocol-relative (//host).
	allowedSchemes: ['http', 'https', 'mailto'],
	allowProtocolRelative: false,
	// Tag non ammessi rimossi mantenendone il testo (come il KEEP_CONTENT di default di DOMPurify).
	disallowedTagsMode: 'discard',
	// I link esterni aperti in nuova scheda non devono poter manipolare la tab d'origine.
	transformTags: {
		a: (tagName, attribs) => ({
			tagName,
			attribs: { ...attribs, target: '_blank', rel: 'noopener noreferrer' }
		})
	}
};

/**
 * Ripulisce una stringa HTML rendendola sicura per il rendering con {@html}.
 * Restituisce stringa vuota se l'input è assente.
 */
export function sanitizeHtml(html?: string | null): string {
	return html ? sanitizeHtmlLib(html, SANITIZE_OPTIONS) : '';
}
