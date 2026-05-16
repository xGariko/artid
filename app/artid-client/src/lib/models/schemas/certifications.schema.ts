import { z } from "zod";

export const CertificationsSchema = z.object({
  id: z.int().positive(),
  idUser: z.int().positive(),
  idFile: z.int().positive().nullable(),
  title: z.string().min(1).max(300),
  description: z.string().max(2000).nullable(),
  isPublic: z.boolean().default(false),
});

export type Certifications = z.infer<typeof CertificationsSchema>;
