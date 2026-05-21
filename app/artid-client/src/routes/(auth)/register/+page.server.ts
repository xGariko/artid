import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { login } from "$lib/server/auth";
import { RegisterRequestSchema } from "$lib/models/schemas";

const API_BASE = "http://localhost:8080";

type RegisterField =
	| "name"
	| "surname"
	| "email"
	| "password"
	| "confirmPassword"
	| "birthdate"
	| "birthplace"
	| "taxId";

type FieldErrors = Partial<Record<RegisterField, string>>;

const FIELD_MESSAGES: Record<RegisterField, string> = {
	name: "Il nome è obbligatorio.",
	surname: "Il cognome è obbligatorio.",
	email: "Inserisci un indirizzo email valido.",
	password: "La password deve contenere almeno 8 caratteri.",
	confirmPassword: "Le password non coincidono.",
	birthdate: "Data di nascita non valida.",
	birthplace: "Luogo di nascita non valido.",
	taxId: "Codice fiscale non valido.",
};

export const actions: Actions = {
	default: async ({ request, cookies }) => {
		const form = await request.formData();
		const name = (form.get("name") as string)?.trim() ?? "";
		const surname = (form.get("surname") as string)?.trim() ?? "";
		const email = (form.get("email") as string)?.trim() ?? "";
		const password = (form.get("password") as string) ?? "";
		const confirmPassword = (form.get("confirmPassword") as string) ?? "";
		const birthdate = ((form.get("birthdate") as string) ?? "").trim();
		const birthplace = ((form.get("birthplace") as string) ?? "").trim();
		const taxId = ((form.get("taxId") as string) ?? "").trim().toUpperCase();

		const formState = { name, surname, email, birthdate, birthplace, taxId };
		const errors: FieldErrors = {};

		const parsed = RegisterRequestSchema.safeParse({
			name,
			surname,
			email,
			password,
			birthdate: birthdate || undefined,
			birthplace: birthplace || undefined,
			taxId,
		});

		if (!parsed.success) {
			for (const issue of parsed.error.issues) {
				const field = issue.path[0] as RegisterField | undefined;
				if (field && !errors[field]) {
					errors[field] = FIELD_MESSAGES[field] ?? issue.message;
				}
			}
		}

		if (password && confirmPassword !== password) {
			errors.confirmPassword = FIELD_MESSAGES.confirmPassword;
		}

		if (Object.keys(errors).length > 0) {
			return fail(400, { errors, ...formState });
		}

		const res = await fetch(`${API_BASE}/api/auth/register`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({
				name,
				surname,
				email,
				password,
				birthdate: birthdate || null,
				birthplace: birthplace || null,
				taxId: taxId || null,
			}),
		});

		if (!res.ok) {
			const conflict = res.status === 409;
			return fail(res.status, {
				errors: conflict
					? ({ email: "Esiste già un account con questa email." } as FieldErrors)
					: ({} as FieldErrors),
				formError: conflict ? undefined : "Errore durante la registrazione. Riprova.",
				...formState,
			});
		}

		const result = await login(cookies, { email, password });

		if (!result.ok) {
			redirect(303, "/login");
		}

		redirect(303, "/dashboard");
	},
};
