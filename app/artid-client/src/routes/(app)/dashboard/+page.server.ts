import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const [artid, resource, certification, share, profile] = await Promise.all([
		locals.api.GET("/api/artids/count"),
		locals.api.GET("/api/resources/count"),
		locals.api.GET("/api/certifications/count"),
		locals.api.GET("/api/shares/count"),
		locals.api.GET("/api/profile/completion"),
	]);

	if (!artid.data || !resource.data || !certification.data || !share.data || !profile.data) {
		throw error(500, "Errore nel caricamento della dashboard");
	}

	return {
		artidCount: artid.data.count ?? 0,
		resourceCount: resource.data.count ?? 0,
		certificationCount: certification.data.count ?? 0,
		shareCount: share.data.count ?? 0,
		profileCompletion: profile.data.percentage ?? 0,
	};
};
