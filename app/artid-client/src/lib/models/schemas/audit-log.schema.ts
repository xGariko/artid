import { z } from "zod";

export const AuditLogSchema = z.object({
  id: z.int().positive(),
  idUser: z.int().positive().nullable(),
  action: z.string().min(1).max(100),
  entityType: z.string().min(1).max(100),
  entityId: z.int().positive().nullable(),
  oldValue: z.record(z.string(), z.unknown()).nullable(),
  newValue: z.record(z.string(), z.unknown()).nullable(),
  ipAddress: z
    .string()
    .regex(/^(\d{1,3}\.){3}\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$/)
    .nullable(),
  userAgent: z.string().max(500).nullable(),
  timestamp: z.iso.datetime(),
});

export type AuditLog = z.infer<typeof AuditLogSchema>;
