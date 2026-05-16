import { z } from "zod";

export const ArtidResourceSchema = z.object({
  idResource: z.int().positive(),
  id: z.int().positive(),
  rank: z.int().nonnegative().default(0),
});

export type ArtidResource = z.infer<typeof ArtidResourceSchema>;
