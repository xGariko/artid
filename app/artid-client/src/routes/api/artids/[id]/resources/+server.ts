import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const POST: RequestHandler = async ({ params, request, locals }) => {
	// 1. Verifichiamo che l'utente sia autenticato nel proxy
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const artidId = Number(params.id);

	// 2. Leggiamo l'id del materiale inviato nel body dal frontend
	const materialId = await request.json();

	try {
		// 3. Inoltriamo la richiesta al backend Java usando locals.api
		const { response } = await locals.api.POST('/api/artids/{id}/resources', {
			params: {
				path: { id: artidId }
			},
			body: materialId
		});

		// Se Spring Boot risponde con un errore (es. 400, 404, 500)
		if (!response.ok) {
			console.error(`Spring Boot ha risposto con status: ${response.status}`);
			throw error(response.status, 'Impossibile associare la risorsa nel database');
		}

		// Tutto è andato a buon fine (200 OK da Java), rispondiamo al frontend
		return json({ success: true });
	} catch (err) {
		console.error("Errore durante l'inoltro della richiesta a Spring Boot:", err);
		throw error(500, 'Errore interno del server proxy');
	}
};
