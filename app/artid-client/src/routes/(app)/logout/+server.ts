import { json } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";
import { logout } from "$lib/auth.ts";

export const POST: RequestHandler = async ({ cookies }) => {
	logout(cookies);
	return json({ ok: true });
};
