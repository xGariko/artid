import { error } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";

export const POST: RequestHandler = async ({ request, locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const body = await request.json();

	const result = await locals.api.POST("/api/resources", { body });
	const response = result.response;

	if (!result.data) {
		throw error(response.status, "Errore nella creazione della risorsa");
	}

	return new Response(JSON.stringify(result.data), {
		status: 200,
		headers: { "content-type": "application/json" },
	});
};
