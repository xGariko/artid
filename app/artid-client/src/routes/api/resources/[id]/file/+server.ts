import { error } from "@sveltejs/kit";
import { env } from "$env/dynamic/private";
import type { RequestHandler } from "./$types";

const API_BASE = env.API_BASE ?? "http://localhost:8080";

export const GET: RequestHandler = async ({ params, locals }) => {
	const token = locals.token;
	if (!token) {
		throw error(401, "Non autenticato");
	}

	const upstream = await fetch(`${API_BASE}/api/resources/${params.id}/file`, {
		headers: { Authorization: `Bearer ${token}` },
	});

	if (upstream.status === 404) {
		throw error(404, "File non trovato");
	}
	if (!upstream.ok) {
		throw error(upstream.status, "Errore nel download del file");
	}

	const headers = new Headers();
	const contentType = upstream.headers.get("content-type");
	if (contentType) headers.set("content-type", contentType);
	const disposition = upstream.headers.get("content-disposition");
	if (disposition) headers.set("content-disposition", disposition);

	return new Response(upstream.body, { status: 200, headers });
};
