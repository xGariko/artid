import { z } from "zod";

export const InternalShareSchema = z.object({
  idUser: z.int().positive(),
  id: z.int().positive(),
});

export type InternalShare = z.infer<typeof InternalShareSchema>;
