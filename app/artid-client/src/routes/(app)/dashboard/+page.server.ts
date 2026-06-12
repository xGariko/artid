import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	// Un solo round-trip: il backend aggrega i conteggi in un'unica transazione read-only,
	// invece di 5 richieste HTTP separate (ognuna con la propria autenticazione).
	const { data } = await locals.api.GET("/api/dashboard/summary");

	if (!data) {
		throw error(500, "Errore nel caricamento della dashboard");
	}

	return {
		artidCount: data.artidCount ?? 0,
		resourceCount: data.resourceCount ?? 0,
		certificationCount: data.certificationCount ?? 0,
		shareCount: data.shareCount ?? 0,
		profileCompletion: data.profileCompletion ?? 0,
	};
};
