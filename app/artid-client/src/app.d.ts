import type { AuthUser } from "$lib/stores/auth";

declare global {
	namespace App {
		interface Locals {
			token: string | null;
			user: AuthUser | null;
		}
	}
}

declare module "bootstrap/dist/js/bootstrap.bundle.min.js";

export {};
