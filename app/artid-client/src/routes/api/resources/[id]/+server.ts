import { error } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";

export const DELETE: RequestHandler = async ({ params, locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const id = Number(params.id);
	if (!Number.isFinite(id)) {
		throw error(400, "ID non valido");
	}

	const { response } = await locals.api.DELETE("/api/resources/{id}", {
		params: { path: { id } },
	});

	if (response.status === 404) {
		throw error(404, "Risorsa non trovata");
	}
	if (!response.ok) {
		throw error(response.status, "Errore nell'eliminazione");
	}

	return new Response(null, { status: 204 });
};

export const PUT: RequestHandler = async ({ params, request, locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const id = Number(params.id);
	if (!Number.isFinite(id)) {
		throw error(400, "ID non valido");
	}

	const body = await request.json();

	const result = await locals.api.PUT("/api/resources/{id}", {
		params: { path: { id } },
		body,
	});
	const response = result.response;

	if (response.status === 404) {
		throw error(404, "Risorsa non trovata");
	}
	if (!result.data) {
		throw error(response.status, "Errore nell'aggiornamento della risorsa");
	}

	return new Response(JSON.stringify(result.data), {
		status: 200,
		headers: { "content-type": "application/json" },
	});
};
