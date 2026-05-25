import { error } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";
import { createApiClient } from "$lib/api/client";

export const load: PageServerLoad = async ({ locals }) => {
	const token = locals.token;
	if (!token) {
		throw error(401, "Non autenticato");
	}

	const api = createApiClient(token);
	const { data, error: apiError } = await api.GET("/api/resources");

	if (apiError || !data) {
		throw error(500, "Errore nel caricamento delle risorse");
	}

	return {
		resources: data,
	};
};
