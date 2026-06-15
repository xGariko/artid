import { error } from '@sveltejs/kit';
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = async ({ params, locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const id = Number(params.id);
	if (!Number.isInteger(id) || id <= 0) {
		throw error(404, 'ArtID non trovato');
	}

	const { data, response } = await locals.api.GET('/api/artids/{id}', {
		params: { path: { id } }
	});

	if (!data) {
		throw error(response.status === 404 ? 404 : 500, 'ArtID non trovato');
	}

	return { artid: data };
};
