import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ params, locals }) => {
	if (!locals.user) {
		throw error(401, 'Non autenticato');
	}

	const id = Number(params.id);

	// Due chiamate server indipendenti, in parallelo:
	//  - /api/profile               → profilo completo dell'utente loggato (risolto dal JWT)
	//  - /api/artids/{id}/resources → materiali legati all'ArtID. Il backend verifica nella query
	//    che ArtID e materiali siano dell'utente del JWT: nessun accesso a materiali altrui.
	const [profileRes, resourcesRes] = await Promise.all([
		locals.api.GET('/api/profile'),
		locals.api.GET('/api/artids/{id}/resources', { params: { path: { id } } })
	]);

	if (!profileRes.data) {
		throw error(500, 'Errore nel caricamento del profilo');
	}
	if (!resourcesRes.data) {
		throw error(resourcesRes.response.status, 'Errore nel caricamento dei materiali');
	}

	return {
		profile: profileRes.data,
		resources: resourcesRes.data
	};
};
