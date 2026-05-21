import { writable } from 'svelte/store';

export const loading = writable<boolean | null>(null);
