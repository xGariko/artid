import { redirect } from "@sveltejs/kit";
import type { Actions } from "./$types";
import { logout } from "$lib/server/auth";

export const actions: Actions = {
	logout: async ({ cookies }) => {
		logout(cookies);
		redirect(303, "/login");
	},
};
