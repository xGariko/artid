import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { login } from "$lib/server/auth";

export const actions: Actions = {
	default: async ({ request, cookies }) => {
		const form = await request.formData();
		const username = form.get("username") as string;
		const password = form.get("password") as string;

		if (!username || !password) {
			return fail(400, { error: "Username e password sono obbligatori.", username });
		}

		const result = await login(cookies, { username, password });

		if (!result.ok) {
			return fail(401, { error: result.error, username });
		}

		redirect(303, "/dashboard");
	},
};
