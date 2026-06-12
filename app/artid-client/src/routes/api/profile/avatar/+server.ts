import { error, json } from "@sveltejs/kit";
import { env } from "$env/dynamic/private";
import type { RequestHandler } from "./$types";

const API_BASE = env.API_BASE ?? "http://localhost:8080";

// Upload foto profilo: inoltra il multipart così com'è a Spring iniettando il JWT dal cookie
// httpOnly. Streaming senza bufferizzare (duplex: "half"), content-type originale col boundary.
export const PUT: RequestHandler = async ({ request, locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const upstream = await fetch(`${API_BASE}/api/profile/avatar`, {
		method: "PUT",
		headers: {
			Authorization: `Bearer ${locals.token}`,
			"content-type": request.headers.get("content-type") ?? "application/octet-stream",
		},
		body: request.body,
		duplex: "half",
	} as RequestInit & { duplex: "half" });

	if (!upstream.ok) {
		throw error(upstream.status, "Errore nel caricamento della foto profilo");
	}

	return json(await upstream.json());
};

export const DELETE: RequestHandler = async ({ locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const upstream = await fetch(`${API_BASE}/api/profile/avatar`, {
		method: "DELETE",
		headers: { Authorization: `Bearer ${locals.token}` },
	});

	if (!upstream.ok) {
		throw error(upstream.status, "Errore nella rimozione della foto profilo");
	}

	return new Response(null, { status: 204 });
};
