import { error } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";
import { logout } from "$lib/auth";

// Proxy same-origin per l'eliminazione account. Oltre a iniettare il JWT (cookie httpOnly),
// impone l'ownership: si può eliminare SOLO il proprio account. Spring espone
// DELETE /api/users/{id} senza controllo di proprietà, quindi senza questo check un utente
// autenticato potrebbe cancellare l'account di un altro passando un id arbitrario.
export const DELETE: RequestHandler = async ({ params, locals, cookies }) => {
	if (!locals.token || !locals.user) {
		throw error(401, "Non autenticato");
	}

	const id = Number(params.id);
	if (!Number.isFinite(id)) {
		throw error(400, "ID non valido");
	}

	if (id !== locals.user.id) {
		throw error(403, "Non puoi eliminare un account diverso dal tuo");
	}

	const { response } = await locals.api.DELETE("/api/users/{id}", {
		params: { path: { id } },
	});

	if (response.status === 404) {
		throw error(404, "Utente non trovato");
	}
	if (!response.ok) {
		throw error(response.status, "Errore nell'eliminazione dell'account");
	}

	// Hai eliminato il tuo stesso account: azzera la sessione (cookie httpOnly + me-cache),
	// altrimenti il token resterebbe valido come firma ma punterebbe a un utente inesistente.
	logout(cookies);

	return new Response(null, { status: 204 });
};
