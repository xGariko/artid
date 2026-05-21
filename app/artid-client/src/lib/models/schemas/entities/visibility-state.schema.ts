import { z } from "zod";

export const VisibilityStateSchema = z.object({
  id: z.int().positive(),
  title: z.string().min(1).max(50),
});

export type VisibilityState = z.infer<typeof VisibilityStateSchema>;
