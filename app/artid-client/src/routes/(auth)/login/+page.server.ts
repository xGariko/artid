import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { login } from "$lib/auth.ts";

type LoginField = "email" | "password";
type FieldErrors = Partial<Record<LoginField, string>>;

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const actions: Actions = {
	default: async ({ request, cookies, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";
		const password = (form.get("password") as string) ?? "";

		const errors: FieldErrors = {};

		if (!email) {
			errors.email = "L'email è obbligatoria.";
		} else if (!EMAIL_REGEX.test(email)) {
			errors.email = "Inserisci un indirizzo email valido.";
		}

		if (!password) {
			errors.password = "La password è obbligatoria.";
		}

		if (Object.keys(errors).length > 0) {
			return fail(400, { errors, email });
		}

		const result = await login(locals.api, cookies, { email, password });

		if (!result.ok) {
			return fail(401, {
				errors: {} as FieldErrors,
				formError: result.error || "Email o password non corretti.",
				email,
			});
		}

		redirect(303, "/dashboard");
	},
};
