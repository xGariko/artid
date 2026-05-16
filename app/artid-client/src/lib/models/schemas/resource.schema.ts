import { z } from "zod";

export const ResourceSchema = z.object({
  id: z.int().positive(),
  idUser: z.int().positive(),
  idVisibilityState: z.int().positive().nullable(),
  idFile: z.int().positive().nullable(),
  title: z.string().min(1).max(300),
  description: z.string().max(5000).nullable(),
  favorite: z.boolean().default(false),
  createdAt: z.iso.datetime(),
  lastModified: z.iso.datetime(),
  deletedAt: z.iso.datetime().nullable(),
});

export type Resource = z.infer<typeof ResourceSchema>;
