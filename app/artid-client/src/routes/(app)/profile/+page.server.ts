import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.user) {
		throw error(401, 'Non autenticato');
	}

	// L'utente è risolto lato backend dal JWT (authentication.getName()),
	// non passiamo né id né email dal client.
	const { data, error: apiError } = await locals.api.GET('/api/profile');

	if (apiError || !data) {
		throw error(500, 'Errore nel caricamento del profilo');
	}

	return { profile: data };
};
