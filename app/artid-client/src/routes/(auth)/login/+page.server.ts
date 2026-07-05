import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { requestOtp, validateCredentials, verifyOtp, resendOtp } from "$lib/auth.ts";

type LoginField = "email" | "password";
type FieldErrors = Partial<Record<LoginField, string>>;

// Forma unica di ritorno: collassa l'union delle action così la pagina accede ai campi senza
// narrowing. `step: "otp"` indica che siamo nella fase di verifica del codice.
type LoginActionData = {
	step?: "otp";
	validated?: boolean;
	email?: string;
	errors?: FieldErrors;
	formError?: string;
	codeError?: string;
	resent?: boolean;
};

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const CODE_REGEX = /^\d{6}$/;

const SESSION_LOST = "Sessione scaduta, ricomincia l'accesso.";
// Messaggio di codice errato/scaduto come da RAD (caso d'uso GENERA OTP).
const INVALID_CODE =
	"Errore: codice non valido, controlla nella mail che non sia scaduto. Se è scaduto clicca Riprova.";

export const actions: Actions = {
	// Step di conferma (RAD): valida le credenziali SENZA inviare l'OTP, così la schermata "Ok"
	// (che poi invia davvero il codice via ?/requestOtp) appare solo con credenziali corrette.
	validate: async ({ request, locals }) => {
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
		} else if (password.length < 8) {
			errors.password = "La password deve avere almeno 8 caratteri."
		}

		if (Object.keys(errors).length > 0) {
			return fail(400, { errors, email } as LoginActionData);
		}

		const result = await validateCredentials(locals.api, { email, password });
		if (!result.ok) {
			return fail(401, { errors: {}, formError: result.error, email } as LoginActionData);
		}

		return { validated: true, email } as LoginActionData;
	},

	// Step 1 (all'"Ok"): rivalida le credenziali e, in caso positivo, invia l'OTP via email.
	requestOtp: async ({ request, locals }) => {
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
			return fail(400, { errors, email } as LoginActionData);
		}

		const result = await requestOtp(locals.api, { email, password });
		if (!result.ok) {
			return fail(401, { errors: {}, formError: result.error, email } as LoginActionData);
		}

		return { step: "otp", email: result.email } as LoginActionData;
	},

	// Step 2: verifica l'OTP e, se valido, apre la sessione e va in home.
	verify: async ({ request, cookies, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";
		const code = (form.get("code") as string)?.trim() ?? "";

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as LoginActionData);
		}
		if (!CODE_REGEX.test(code)) {
			return fail(400, {
				step: "otp",
				email,
				codeError: "Inserisci il codice OTP a 6 cifre.",
			} as LoginActionData);
		}

		const result = await verifyOtp(locals.api, cookies, { email, code });
		if (!result.ok) {
			return fail(401, { step: "otp", email, codeError: INVALID_CODE } as LoginActionData);
		}

		redirect(303, "/home");
	},

	// "Riprova": rigenera e rinvia l'OTP, restando nella fase di verifica.
	resend: async ({ request, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as LoginActionData);
		}

		await resendOtp(locals.api, email);
		return { step: "otp", email, resent: true } as LoginActionData;
	},
};
