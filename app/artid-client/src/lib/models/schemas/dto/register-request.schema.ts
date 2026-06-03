import { z } from "zod";

export const RegisterRequestSchema = z.object({
	name: z.string().min(1).max(100),
	surname: z.string().min(1).max(100),
	email: z.email(),
	password: z.string().min(8).max(100),
	birthdate: z.iso.date().optional(),
	birthplace: z.string().max(200).optional(),
});

export type RegisterRequest = z.infer<typeof RegisterRequestSchema>;
