import { z } from "zod";

export const FileSchema = z.object({
  id: z.int().positive(),
  filePath: z.string().min(1).max(1000),
  fileName: z.string().min(1).max(255),
  extension: z.string().max(20).nullable(),
  mimeType: z
    .string()
    .regex(/^[\w-]+\/[\w\-+.]+$/)
    .max(100)
    .nullable(),
});

export type File = z.infer<typeof FileSchema>;
