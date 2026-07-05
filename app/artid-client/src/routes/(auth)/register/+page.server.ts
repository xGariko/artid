import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import {
	requestRegistration,
	validateRegistrationEmail,
	verifyRegistration,
	resendRegistrationOtp,
} from "$lib/auth";
import { RegisterRequestSchema } from "$lib/models/schemas";

type RegisterField =
	| "name"
	| "surname"
	| "email"
	| "password"
	| "confirmPassword"
	| "birthdate"
	| "birthplace";

type FieldErrors = Partial<Record<RegisterField, string>>;

// Forma unica di ritorno (come nel login): `step: "otp"` indica la fase di verifica del codice.
// I campi del form vengono rimandati indietro per ripopolarli in caso di errore.
type RegisterActionData = {
	step?: "otp";
	validated?: boolean;
	email?: string;
	name?: string;
	surname?: string;
	birthdate?: string;
	birthplace?: string;
	errors?: FieldErrors;
	formError?: string;
	codeError?: string;
	resent?: boolean;
};

const FIELD_MESSAGES: Record<RegisterField, string> = {
	name: "Il nome è obbligatorio.",
	surname: "Il cognome è obbligatorio.",
	email: "Inserisci un indirizzo email valido.",
	password: "La password deve contenere almeno 8 caratteri.",
	confirmPassword: "Le password non coincidono.",
	birthdate: "Inserisci una data di nascita valida e antecedente a oggi.",
	birthplace: "Luogo di nascita non valido.",
};

const CODE_REGEX = /^\d{6}$/;
const SESSION_LOST = "Sessione scaduta, ricomincia la registrazione.";
// Messaggio di codice errato/scaduto come da RAD (caso d'uso GENERA OTP).
const INVALID_CODE =
	"Errore: codice non valido, controlla nella mail che non sia scaduto. Se è scaduto clicca Riprova.";

export const actions: Actions = {
	// Step di conferma (RAD): valida i dati e la disponibilità dell'email SENZA inviare l'OTP né
	// creare il pending, così la schermata "Ok" (che poi invia il codice via ?/requestOtp) appare
	// solo con dati validi ed email libera.
	validate: async ({ request, locals }) => {
		const form = await request.formData();
		const name = (form.get("name") as string)?.trim() ?? "";
		const surname = (form.get("surname") as string)?.trim() ?? "";
		const email = (form.get("email") as string)?.trim() ?? "";
		const password = (form.get("password") as string) ?? "";
		const confirmPassword = (form.get("confirmPassword") as string) ?? "";
		const birthdate = ((form.get("birthdate") as string) ?? "").trim();
		const birthplace = ((form.get("birthplace") as string) ?? "").trim();

		const formState = { name, surname, email, birthdate, birthplace };
		const errors: FieldErrors = {};

		const parsed = RegisterRequestSchema.safeParse({
			name,
			surname,
			email,
			password,
			birthdate: birthdate || undefined,
			birthplace: birthplace || undefined,
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
			return fail(400, { errors, ...formState } as RegisterActionData);
		}

		const result = await validateRegistrationEmail(locals.api, {
			name,
			surname,
			email,
			password,
			birthdate: birthdate || undefined,
			birthplace: birthplace || undefined,
		});

		if (!result.ok) {
			return fail(result.field === "email" ? 409 : 400, {
				errors: result.field === "email" ? ({ email: result.error } as FieldErrors) : ({} as FieldErrors),
				formError: result.field === "email" ? undefined : result.error,
				...formState,
			} as RegisterActionData);
		}

		return { validated: true, ...formState } as RegisterActionData;
	},

	// Step 1 (all'"Ok"): rivalida i dati e, se l'email è libera, invia l'OTP di verifica (l'account
	// NON è ancora creato: nasce solo dopo la verifica del codice).
	requestOtp: async ({ request, locals }) => {
		const form = await request.formData();
		const name = (form.get("name") as string)?.trim() ?? "";
		const surname = (form.get("surname") as string)?.trim() ?? "";
		const email = (form.get("email") as string)?.trim() ?? "";
		const password = (form.get("password") as string) ?? "";
		const confirmPassword = (form.get("confirmPassword") as string) ?? "";
		const birthdate = ((form.get("birthdate") as string) ?? "").trim();
		const birthplace = ((form.get("birthplace") as string) ?? "").trim();

		const formState = { name, surname, email, birthdate, birthplace };
		const errors: FieldErrors = {};

		const parsed = RegisterRequestSchema.safeParse({
			name,
			surname,
			email,
			password,
			birthdate: birthdate || undefined,
			birthplace: birthplace || undefined,
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
			return fail(400, { errors, ...formState } as RegisterActionData);
		}

		const result = await requestRegistration(locals.api, {
			name,
			surname,
			email,
			password,
			birthdate: birthdate || undefined,
			birthplace: birthplace || undefined,
		});

		if (!result.ok) {
			// Email già registrata → errore sul campo; altri casi → errore generale.
			return fail(result.field === "email" ? 409 : 400, {
				errors: result.field === "email" ? ({ email: result.error } as FieldErrors) : ({} as FieldErrors),
				formError: result.field === "email" ? undefined : result.error,
				...formState,
			} as RegisterActionData);
		}

		return { step: "otp", email: result.email, ...formState } as RegisterActionData;
	},

	// Step 2: verifica l'OTP e, se valido, crea l'account, apre la sessione e va in dashboard.
	verify: async ({ request, cookies, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";
		const code = (form.get("code") as string)?.trim() ?? "";

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as RegisterActionData);
		}
		if (!CODE_REGEX.test(code)) {
			return fail(400, {
				step: "otp",
				email,
				codeError: "Inserisci il codice OTP a 6 cifre.",
			} as RegisterActionData);
		}

		const result = await verifyRegistration(locals.api, cookies, { email, code });
		if (!result.ok) {
			return fail(401, { step: "otp", email, codeError: INVALID_CODE } as RegisterActionData);
		}

		redirect(303, "/dashboard");
	},

	// "Riprova": rigenera e rinvia l'OTP, restando nella fase di verifica.
	resend: async ({ request, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as RegisterActionData);
		}

		await resendRegistrationOtp(locals.api, email);
		return { step: "otp", email, resent: true } as RegisterActionData;
	},
};
