import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const [artids, tags] = await Promise.all([
		locals.api.GET('/api/artids'),
		locals.api.GET('/api/tags')
	]);

	if (!artids.data) {
		throw error(500, 'Errore nel caricamento della pagina artid');
	}

	return {
		artids: artids.data,
		tags: tags.data
	};
};
