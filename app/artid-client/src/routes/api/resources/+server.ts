import { error, json } from '@sveltejs/kit';
import { env } from '$env/dynamic/private';
import type { RequestHandler } from './$types';

const API_BASE = env.API_BASE ?? 'http://localhost:8080';

export const POST: RequestHandler = async ({ request, locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	// Inoltra il multipart così com'è a Spring, iniettando il JWT dal cookie httpOnly.
	// Preserviamo il content-type originale (contiene il boundary) e facciamo streaming
	// del body senza bufferizzarlo (duplex: "half"). Niente openapi-fetch qui: serializza in JSON.
	const upstream = await fetch(`${API_BASE}/api/resources`, {
		method: 'POST',
		headers: {
			Authorization: `Bearer ${locals.token}`,
			'content-type': request.headers.get('content-type') ?? 'application/octet-stream'
		},
		body: request.body,
		duplex: 'half'
	} as RequestInit & { duplex: 'half' });

	if (!upstream.ok) {
		throw error(upstream.status, 'Errore nella creazione della risorsa');
	}

	return new Response(await upstream.text(), {
		status: 200,
		headers: { 'content-type': 'application/json' }
	});
};

export const GET: RequestHandler = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const { data, response } = await locals.api.GET('/api/resources');

	if (!data) {
		throw error(response.status, 'Errore nel caricamento dei materiali');
	}

	return json(data);
};
