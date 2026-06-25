import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	const userId = Number(params.id);
	const artidId = Number(params.artidId);
	if (!Number.isInteger(userId) || userId <= 0 || !Number.isInteger(artidId) || artidId <= 0) {
		throw error(404, 'ArtID non trovato');
	}

	// Endpoint pubblico: nessun token. 404 = ArtID inesistente, non pubblico o non di questo utente
	// (indistinguibili di proposito, come per il dettaglio profilo).
	const { data, response } = await locals.api.GET('/api/users/{userId}/public/artids/{artidId}', {
		params: { path: { userId, artidId } }
	});

	if (!data) {
		throw error(response.status === 404 ? 404 : 500, 'ArtID non trovato');
	}

	return { artid: data };
};
