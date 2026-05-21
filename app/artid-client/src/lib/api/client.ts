import createClient from "openapi-fetch";
import { env } from "$env/dynamic/private";
import type { paths } from "./schema";

const API_BASE = env.API_BASE ?? "http://localhost:8080";

export function createApiClient(token?: string | null) {
	return createClient<paths>({
		baseUrl: API_BASE,
		headers: token ? { Authorization: `Bearer ${token}` } : {},
	});
}
