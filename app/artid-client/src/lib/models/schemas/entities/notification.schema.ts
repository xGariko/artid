import { z } from "zod";

export const NotificationSchema = z.object({
  id: z.int().positive(),
  idUser: z.int().positive(),
  message: z.string().min(1).max(2000),
  createdAt: z.iso.datetime(),
  readAt: z.iso.datetime().nullable(),
});

export type Notification = z.infer<typeof NotificationSchema>;
