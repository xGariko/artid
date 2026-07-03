// Tipi derivati dal contratto OpenAPI (schema.d.ts, generato da `npm run api:generate`).
// Qui centralizziamo SOLO i tipi di risposta / read-model restituiti dal backend.
// Per le richieste che validi lato client (es. ProfileUpdateRequest, RegisterRequest)
// la fonte di verità resta lo schema zod in $lib/models/schemas: non vanno ri-aliasate
// da qui, o avresti due tipi omonimi che possono divergere.

import type { components } from "./schema";

type Schemas = components["schemas"];

/**
 * Jolly per i read-model usati di rado: `Schema<"CountResponse">` senza doverli
 * censire qui sotto uno per uno.
 */
export type Schema<K extends keyof Schemas> = Schemas[K];

// Read-model restituiti dal backend.
export type Profile = Schemas["ProfileResponse"];
export type ProfileCompletion = Schemas["ProfileCompletionResponse"];
export type ResourceResponse = Schemas["ResourceResponse"];
export type ArtidResponse = Schemas["ArtidResponse"];
export type DashboardSummary = Schemas["DashboardSummaryResponse"];
export type AuthResponse = Schemas["AuthResponse"];
export type UserResponse = Schemas["UserResponse"];
export type CountResponse = Schemas["CountResponse"];
export type PublicProfile = Schemas["PublicProfileResponse"];
export type PublicProfileDetail = Schemas["PublicProfileDetailResponse"];
export type PublicArtidSummary = Schemas["PublicArtidSummaryResponse"];
export type PublicArtidDetail = Schemas["PublicArtidDetailResponse"];
export type PublicMaterial = Schemas["PublicMaterialResponse"];
export type PublicCertification = Schemas["PublicCertificationResponse"];
export type InternalShareArtIDResponse = Schemas["InternalShareArtIDResponse"];
export type ExternalShareArtIDResponse = Schemas["ExternalShareArtIDResponse"];
export type CertificationResponse = Schemas["CertificationResponse"];
