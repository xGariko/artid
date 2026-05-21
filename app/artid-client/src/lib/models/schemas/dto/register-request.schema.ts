import { z } from "zod";

export const RegisterRequestSchema = z.object({
	name: z.string().min(1).max(100),
	surname: z.string().min(1).max(100),
	email: z.email(),
	password: z.string().min(8).max(100),
	birthdate: z.iso.date().optional(),
	birthplace: z.string().max(200).optional(),
	taxId: z
		.string()
		.regex(/^[A-Z]{6}\d{2}[A-Z]\d{2}[A-Z]\d{3}[A-Z]$/i)
});

export type RegisterRequest = z.infer<typeof RegisterRequestSchema>;
