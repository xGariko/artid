import createClient from "openapi-fetch";
import type { paths } from "./schema";

export function createApiClient(token?: string | null) {
	return createClient<paths>({
		baseUrl: "http://localhost:8080",
		headers: token ? { Authorization: `Bearer ${token}` } : {},
	});
}
