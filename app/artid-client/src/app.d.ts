import type { User } from "$lib/models/schemas";

declare global {
	namespace App {
		interface Locals {
			token: string | null;
			user: User | null;
		}
	}
}

declare module "bootstrap/dist/js/bootstrap.bundle.min.js";

export {};
