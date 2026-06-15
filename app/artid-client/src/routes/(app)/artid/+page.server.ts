import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const artids = await locals.api.GET('/api/artids');

	// const [resources, artids] = await Promise.all([
	//     locals.api.GET("/api/resources"),
	//     locals.api.GET("/api/artids"),
	// ]);

	if (!artids.data) {
		throw error(500, 'Errore nel caricamento della pagina risorse');
	}

	return {
		// resources: resources.data,
		artids: artids.data
	};
};
