import { z } from "zod";

export const ArtidSchema = z.object({
  id: z.int().positive(),
  idUser: z.int().positive(),
  idTag: z.int().positive().nullable(),
  idThumbnail: z.int().positive().nullable(),
  title: z.string().min(1).max(300),
  favourite: z.boolean().default(false),
  createdAt: z.iso.datetime(),
  lastModified: z.iso.datetime(),
  deletedAt: z.iso.datetime().nullable(),
});

export type Artid = z.infer<typeof ArtidSchema>;
