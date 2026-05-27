import adapter from '@sveltejs/adapter-vercel';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	compilerOptions: {
		runes: ({ filename }) => (filename.split(/[/\\]/).includes('node_modules') ? undefined : true)
	},
	kit: {
		adapter: adapter()
	},

	/*
	IMPORTANTE: Questa opzione obbliga a usare il resolve() di svelte per ogni redirect
	Questo aiuta a gestire le router-guards nel modo corretto, rendendo più solida l'applicazione
	* */
	rules: {
		'svelte/no-navigation-without-resolve': 'on'
	}
};

export default config;
