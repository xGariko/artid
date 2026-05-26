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
