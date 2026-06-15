import { z } from "zod";

export const UserSchema = z.object({
  id: z.int().positive(),
  name: z.string().min(1).max(100),
  surname: z.string().min(1).max(100),
  mail: z.email(),
  passwordHash: z.string().min(1),
  birthdate: z.iso.date().nullable(),
  birthplace: z.string().max(200).nullable(),
  address: z.string().max(500).nullable(),
  biography: z.string().max(5000).nullable(),
  linkedinId: z.string().max(200).nullable(),
  facebookId: z.string().max(200).nullable(),
  instagramId: z.string().max(200).nullable(),
  profession: z.string().max(200).nullable(),
  isPublic: z.boolean().default(false),
  phone: z
    .string()
    .regex(/^\+?[\d\s\-()]{5,20}$/)
    .nullable(),
  businessEmail: z.email().nullable(),
  deletedAt: z.iso.datetime().nullable(),
});

export type User = z.infer<typeof UserSchema>;
