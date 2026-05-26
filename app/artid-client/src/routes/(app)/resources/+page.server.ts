import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const { data, error: apiError } = await locals.api.GET("/api/resources");

	if (apiError || !data) {
		throw error(500, "Errore nel caricamento delle risorse");
	}

	return {
		resources: data,
	};
};
