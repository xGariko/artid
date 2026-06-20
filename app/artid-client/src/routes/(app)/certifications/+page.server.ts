import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const [certifications] = await Promise.all([
		locals.api.GET("/api/certifications"),
	]);

	if (!certifications.data) {
		throw error(500, "Errore nel caricamento della pagina certificati");
	}

	return {
		resources: certifications.data,
	};
};
