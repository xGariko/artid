import { error, json } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";

// Proxy del catalogo pubblico: rotta aperta. Inoltra a Spring col JWT se presente (altrimenti
// anonima); lato Spring /api/users/search è permitAll, quindi non serve l'autenticazione.
export const GET: RequestHandler = async ({ url, locals }) => {
	const query = url.searchParams.get("query") ?? "";

	const { data, response } = await locals.api.GET("/api/users/search", {
		params: { query: { query } },
	});

	if (!data) {
		throw error(response.status, "Errore nella ricerca dei profili");
	}

	return json(data);
};
