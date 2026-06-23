import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const [externals, internals] = await Promise.all([
		locals.api.GET('/api/shares/external'),
		locals.api.GET('/api/shares/internal')
	]);

	if (!externals.data || !internals.data) {
		throw error(500, 'Errore nel caricamento della pagina shares');
	}

	return {
		externals: externals.data,
		internals: internals.data
	};
};
