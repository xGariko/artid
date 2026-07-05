import { z } from "zod";
import { toDateInputValue } from "$lib/utilities";

// La data di nascita, se presente, dev'essere ragionevolmente antecedente a oggi: strettamente nel
// passato e non oltre 120 anni fa (esclude refusi come anni futuri o palesemente implausibili).
// Confronto tra stringhe YYYY-MM-DD (formato già garantito da z.iso.date()) per evitare sfasamenti
// di fuso orario.
const MAX_AGE_YEARS = 120;

function isReasonableBirthdate(value: string): boolean {
	const today = new Date();
	const earliest = new Date(today.getFullYear() - MAX_AGE_YEARS, today.getMonth(), today.getDate());
	return value < toDateInputValue(today) && value >= toDateInputValue(earliest);
}

export const RegisterRequestSchema = z
	.object({
		name: z.string().min(1).max(100),
		surname: z.string().min(1).max(100),
		email: z.email(),
		password: z.string().min(8).max(100),
		confirmPassword: z.string().min(1).max(100),
		birthdate: z.iso.date().refine(isReasonableBirthdate),
		birthplace: z.string().min(1).max(200)
	})
	.refine((data) => data.password === data.confirmPassword, {
		message: 'Le password non corrispondono',
		path: ['confirmPassword'] // Associa l'errore al campo confirmPassword
	});

export type RegisterRequest = z.infer<typeof RegisterRequestSchema>;
