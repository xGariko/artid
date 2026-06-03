import { error, json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { ProfileUpdateRequestSchema } from '$lib/models/schemas';

export const PUT: RequestHandler = async ({ request, locals }) => {
	if (!locals.token) {
		throw error(401, 'Non autenticato');
	}

	const body = await request.json();

	// Validazione server-side (il client può sempre bypassare la sua).
	const parsed = ProfileUpdateRequestSchema.safeParse(body);
	if (!parsed.success) {
		const errors: Record<string, string> = {};
		for (const issue of parsed.error.issues) {
			const field = issue.path[0] as string | undefined;
			if (field && !errors[field]) errors[field] = issue.message;
		}
		return json({ errors }, { status: 400 });
	}

	// Inoltro a Spring col Bearer iniettato da locals.api: l'utente aggiornato
	// è SEMPRE quello del JWT (authentication.getName()), mai un id dal body.
	const { data, response } = await locals.api.PUT('/api/profile', { body: parsed.data });

	if (!data) {
		throw error(response.status, "Errore nell'aggiornamento del profilo");
	}

	return json(data);
};
