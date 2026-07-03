import { env } from '$env/dynamic/private';
import type { RequestHandler } from './$types';

const API_BASE = env.API_BASE ?? 'http://localhost:8080';

// Header in entrata da NON re-inoltrare: host/connection sono per il hop browser→SvelteKit,
// content-length lo ricalcola fetch sullo stream, il cookie di sessione non serve a Spring.
// origin/referer: questa è una chiamata server→server, NON una richiesta cross-origin del browser;
// inoltrarli farebbe scattare il filtro CORS di Spring ("Invalid CORS request") quando l'origine di
// deploy (es. https://artid.space) non è tra quelle consentite.
const STRIP_REQUEST = ['host', 'connection', 'content-length', 'cookie', 'origin', 'referer'];

// Proxy generico verso Spring: vale per QUALSIASI /api/* non coperto da una rotta più specifica
// (le rotte con validazione/ownership hanno priorità su questa). Sostituisce il proxy-per-endpoint:
// implementi l'endpoint su Spring e lo chiami subito col browser-client, senza scrivere altro.
// Inietta il Bearer dal cookie httpOnly (che il browser non può leggere) e fa streaming di
// richiesta e risposta senza bufferizzare (duplex: "half"), così multipart e download passano intatti.
const forward: RequestHandler = async ({ request, params, url, locals }) => {
	const headers = new Headers(request.headers);
	for (const header of STRIP_REQUEST) headers.delete(header);
	if (locals.token) headers.set('Authorization', `Bearer ${locals.token}`);

	const hasBody = request.method !== 'GET' && request.method !== 'HEAD';
	const upstream = await fetch(`${API_BASE}/api/${params.path}${url.search}`, {
		method: request.method,
		headers,
		body: hasBody ? request.body : undefined,
		duplex: 'half'
	} as RequestInit & { duplex: 'half' });

	// fetch (undici) decomprime già il body: ripropagare content-encoding e la content-length
	// della versione compressa romperebbe la decodifica lato browser. Le tolgo solo in quel caso.
	const responseHeaders = new Headers(upstream.headers);
	const wasCompressed = responseHeaders.has('content-encoding');
	responseHeaders.delete('content-encoding');
	responseHeaders.delete('transfer-encoding');
	responseHeaders.delete('connection');
	if (wasCompressed) responseHeaders.delete('content-length');

	return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
};

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;
