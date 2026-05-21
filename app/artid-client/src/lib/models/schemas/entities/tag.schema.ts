import { z } from "zod";

export const TagSchema = z.object({
  id: z.int().positive(),
  title: z.string().min(1).max(100),
  color: z
    .string()
    .regex(/^#[0-9a-fA-F]{6}$/)
    .nullable(),
});

export type Tag = z.infer<typeof TagSchema>;
