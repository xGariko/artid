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

	const [artidResponse, materials, tags] = await Promise.all([
		locals.api.GET('/api/artids/{id}', {
			params: { path: { id } }
		}),
		locals.api.GET('/api/artids/{id}/resources', { params: { path: { id } } }),
		locals.api.GET('/api/artids/{id}/tags', { params: { path: { id } } })
	]);

	if (!artidResponse.data) {
		throw error(artidResponse.response.status === 404 ? 404 : 500, 'ArtID non trovato');
	}

	return { artid: artidResponse.data, materials: materials.data, tags: tags.data };
};
