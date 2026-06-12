import { z } from "zod";

export const ProfileUpdateRequestSchema = z.object({
	name: z.string().min(1).max(100),
	surname: z.string().min(1).max(100),
	birthdate: z.iso.date().optional(),
	birthplace: z.string().max(200).optional(),
	address: z.string().max(500).optional(),
	biography: z.string().max(20000).optional(), // HTML di Quill: più lungo del testo puro
	linkedinId: z.string().max(200).optional(),
	facebookId: z.string().max(200).optional(),
	instagramId: z.string().max(200).optional(),
	profession: z.string().max(200).optional(),
	isPublic: z.boolean().optional(),
	phone: z
		.string()
		.regex(/^\+?[\d\s\-()]{5,20}$/)
		.optional(),
	// propic: immagine codificata Base64 (il byte[] del backend). ~4M char ≈ 3MB raw.
	propic: z.string().max(4_000_000).optional(),
	internalShareEnabled: z.boolean().optional(),
});

export type ProfileUpdateRequest = z.infer<typeof ProfileUpdateRequestSchema>;
