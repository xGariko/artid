import { writable } from "svelte/store";

export interface AuthUser {
	id: number;
	email: string;
	name: string;
	surname: string;
}

export const user = writable<AuthUser | null>(null);
