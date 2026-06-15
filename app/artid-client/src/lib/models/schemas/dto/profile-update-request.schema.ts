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
	businessEmail: z.email().max(254).optional(),
	// La foto profilo NON passa più da qui: upload/rimozione via /api/profile/avatar.
	internalShareEnabled: z.boolean().optional(),
});

export type ProfileUpdateRequest = z.infer<typeof ProfileUpdateRequestSchema>;
