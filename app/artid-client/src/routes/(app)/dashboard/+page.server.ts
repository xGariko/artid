import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";
import { createApiClient } from "$lib/api/client";

export const load: PageServerLoad = async ({ locals }) => {
	const token = locals.token;
	if (!token) {
		throw error(401, "Non autenticato");
	}

	const api = createApiClient(token);

	const [artid, resource, certification, share, profile] = await Promise.all([
		api.GET("/api/artids/count"),
		api.GET("/api/resources/count"),
		api.GET("/api/certifications/count"),
		api.GET("/api/shares/count"),
		api.GET("/api/profile/completion"),
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
