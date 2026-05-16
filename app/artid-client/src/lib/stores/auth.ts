import { writable } from "svelte/store";
import type { User } from "$lib/models/schemas";

export const user = writable<User | null>(null);
