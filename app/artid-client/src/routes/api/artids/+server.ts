import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import type { components } from '$lib/api/schema';

// Proxy di creazione ArtID: inoltra a Spring via locals.api (che inietta il JWT dal cookie
// httpOnly). Il client browser parla solo same-origin, mai diretto a Spring. Il body resta
// tipizzato dallo schema OpenAPI; la validazione (titolo non vuoto) la fa il backend → 400.
export const POST: RequestHandler = async ({ request, locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const body = (await request.json()) as components['schemas']['ArtidCreateRequest'];

	const { data, response } = await locals.api.POST('/api/artids', { body });

	if (!data) {
		throw error(response.status, "Errore nella creazione dell'ArtID");
	}

	return json(data);
};
