import { error } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";

// Proxy verso Spring per ottenere un URL presigned MinIO. Il body è {fileName, mimeType};
// la response include l'uploadUrl che il browser usa direttamente per la PUT (i bytes del
// file non passano da SvelteKit).
export const POST: RequestHandler = async ({ request, locals }) => {
	if (!locals.token) {
		throw error(401, "Non autenticato");
	}

	const body = await request.json();

	const result = await locals.api.POST("/api/resources/upload-intent", { body });
	const response = result.response;

	if (!result.data) {
		throw error(response.status, "Errore nella generazione URL di upload");
	}

	return new Response(JSON.stringify(result.data), {
		status: 200,
		headers: { "content-type": "application/json" },
	});
};
