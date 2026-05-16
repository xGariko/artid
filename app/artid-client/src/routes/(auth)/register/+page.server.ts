import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { login } from "$lib/server/auth";

export const actions: Actions = {
	default: async ({ request, cookies }) => {
		const form = await request.formData();
		const username = form.get("username") as string;
		const email = form.get("email") as string;
		const password = form.get("password") as string;
		const confirmPassword = form.get("confirmPassword") as string;

		if (!username || !email || !password) {
			return fail(400, { error: "Tutti i campi sono obbligatori.", username, email });
		}

		if (password !== confirmPassword) {
			return fail(400, { error: "Le password non coincidono.", username, email });
		}

		const res = await fetch("http://localhost:8080/api/auth/register", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ username, email, password }),
		});

		if (!res.ok) {
			return fail(res.status, { error: "Errore durante la registrazione.", username, email });
		}

		const result = await login(cookies, { username, password });

		if (!result.ok) {
			redirect(303, "/login");
		}

		redirect(303, "/dashboard");
	},
};
