import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ params, locals }) => {
	const id = Number(params.id);
	if (!Number.isInteger(id) || id <= 0) {
		throw error(404, "Profilo non trovato");
	}

	// Endpoint pubblico: nessun token richiesto. locals.api parla diretto a Spring.
	// 404 dal backend = profilo inesistente, privato o eliminato (indistinguibili di proposito).
	const { data, response } = await locals.api.GET("/api/users/{id}/public", {
		params: { path: { id } },
	});

	if (!data) {
		throw error(response.status === 404 ? 404 : 500, "Profilo non trovato");
	}

	return { profile: data };
};
