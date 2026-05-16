import { z } from "zod";

export const ExternalShareSchema = z.object({
  id: z.int().positive(),
  idArtid: z.int().positive(),
  clickCounter: z.int().nonnegative().default(0),
  isActive: z.boolean().default(true),
  expirationDate: z.iso.datetime().nullable(),
  lastOpened: z.iso.datetime().nullable(),
});

export type ExternalShare = z.infer<typeof ExternalShareSchema>;
