<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';

	let { children } = $props();

	async function handleLogout() {
		await fetch('/logout', { method: 'POST' });
		await goto(resolve('/login'));
	}
</script>

	<ArtidButton label="Logout" onclick={handleLogout}/>
	<main class="flex-grow-1 p-4 bg-light overflow-auto">
		{@render children()}
	</main>
