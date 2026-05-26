import type { AuthUser } from "$lib/stores/auth";
import type { ApiClient } from "$lib/api/client";

declare global {
	namespace App {
		interface Locals {
			token: string | null;
			user: AuthUser | null;
			api: ApiClient;
		}
	}
}

declare module "bootstrap/dist/js/bootstrap.bundle.min.js";

export {};
