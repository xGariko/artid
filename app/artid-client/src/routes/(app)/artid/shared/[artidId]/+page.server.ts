import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const artidId = Number(params.artidId);
	if (!Number.isInteger(artidId) || artidId <= 0) {
		throw error(404, 'ArtID non trovato');
	}

	// Dettaglio di un ArtID condiviso internamente CON ME. L'accesso è concesso dalla condivisione
	// (id_user_to = utente loggato, accettata), non dalla visibilità: funziona anche per ArtID
	// unlisted/privati, a differenza della vista Explore. 404 se non è condiviso con me.
	const { data, response } = await locals.api.GET('/api/shares/internal/{artidId}', {
		params: { path: { artidId } }
	});

	if (!data) {
		throw error(response.status === 404 ? 404 : 500, 'ArtID non trovato');
	}

	return { detail: data };
};
