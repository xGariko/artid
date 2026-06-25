import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const id = Number(params.id);

	// Stesso read-model della vista Explore (autore, thumbnail, materiali con presigned URL), ma per
	// il proprietario: visibile anche se l'ArtID non è pubblico. La proprietà è verificata dal backend.
	const { data, response } = await locals.api.GET('/api/artids/{id}/preview', {
		params: { path: { id } }
	});

	if (!data) {
		throw error(response.status === 404 ? 404 : 500, 'ArtID non trovato');
	}

	return { detail: data };
};
