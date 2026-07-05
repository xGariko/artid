import { fail, redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { requestPasswordReset, resetPassword, resendOtp } from "$lib/auth.ts";

// Forma unica di ritorno: collassa l'union delle action così la pagina accede ai campi senza
// narrowing. `step: "otp"` indica che siamo nella fase di verifica del codice.
type ForgotActionData = {
	step?: "otp";
	email?: string;
	emailError?: string;
	formError?: string;
	codeError?: string;
	resent?: boolean;
};

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const CODE_REGEX = /^\d{6}$/;

const SESSION_LOST = "Sessione scaduta, ricomincia il recupero.";
// Messaggio di codice errato/scaduto come da RAD (caso d'uso GENERA OTP).
const INVALID_CODE =
	"Errore: codice non valido, controlla nella mail che non sia scaduto. Se è scaduto clicca Riprova.";
const SEND_ERROR = "Impossibile inviare il codice OTP. Riprova tra poco.";

export const actions: Actions = {
	// Step 1: valida l'email; se esiste un account il server invia l'OTP. Se non esiste (404) si torna
	// al LOGIN con il messaggio esplicito "Non esiste un account con questa email" (scelta RAD).
	requestReset: async ({ request, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";

		// Messaggi di validazione verbatim dal RAD (caso d'uso DIM PASS, passo 6).
		if (!email) {
			return fail(400, {
				emailError: "Errore: Bisogna compilare il campo dell'email!",
			} as ForgotActionData);
		}
		if (!EMAIL_REGEX.test(email)) {
			return fail(400, {
				email,
				emailError: "Errore: L'email non è nel formato corretto! Usa nome@dominio.ext.",
			} as ForgotActionData);
		}

		const result = await requestPasswordReset(locals.api, email);
		if (!result.ok) {
			if (result.status === "notFound") {
				redirect(303, "/login?msg=account-not-found");
			}
			return fail(502, { email, formError: SEND_ERROR } as ForgotActionData);
		}

		return { step: "otp", email: result.email } as ForgotActionData;
	},

	// Step 2: verifica l'OTP; se valido la password viene resettata e inviata via email → LOGIN.
	verify: async ({ request, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";
		const code = (form.get("code") as string)?.trim() ?? "";

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as ForgotActionData);
		}
		if (!CODE_REGEX.test(code)) {
			return fail(400, {
				step: "otp",
				email,
				codeError: "Inserisci il codice OTP a 6 cifre.",
			} as ForgotActionData);
		}

		const result = await resetPassword(locals.api, { email, code });
		if (!result.ok) {
			return fail(401, { step: "otp", email, codeError: INVALID_CODE } as ForgotActionData);
		}

		redirect(303, "/login?msg=reset-success");
	},

	// "Riprova": rigenera e rinvia l'OTP (riusa l'endpoint di login), restando nella fase di verifica.
	resend: async ({ request, locals }) => {
		const form = await request.formData();
		const email = (form.get("email") as string)?.trim() ?? "";

		if (!email) {
			return fail(400, { formError: SESSION_LOST } as ForgotActionData);
		}

		await resendOtp(locals.api, email);
		return { step: "otp", email, resent: true } as ForgotActionData;
	},
};
