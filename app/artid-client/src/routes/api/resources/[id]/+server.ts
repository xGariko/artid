import { error } from "@sveltejs/kit";
import { env } from "$env/dynamic/private";
import type { RequestHandler } from "./$types";

const API_BASE = env.API_BASE ?? "http://localhost:8080";

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

	// Inoltra il multipart a Spring (vedi /api/resources POST per i dettagli sullo streaming).
	const upstream = await fetch(`${API_BASE}/api/resources/${id}`, {
		method: "PUT",
		headers: {
			Authorization: `Bearer ${locals.token}`,
			"content-type": request.headers.get("content-type") ?? "application/octet-stream",
		},
		body: request.body,
		duplex: "half",
	} as RequestInit & { duplex: "half" });

	if (upstream.status === 404) {
		throw error(404, "Risorsa non trovata");
	}
	if (!upstream.ok) {
		throw error(upstream.status, "Errore nell'aggiornamento della risorsa");
	}

	return new Response(await upstream.text(), {
		status: 200,
		headers: { "content-type": "application/json" },
	});
};
