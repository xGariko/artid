<script lang="ts">
	import type { Snippet } from 'svelte';
	import { fade } from 'svelte/transition';

	let {
		isOpen = $bindable(),
		children
	}: {
		isOpen: boolean,
		children: Snippet
	} = $props();

	function close() {
		isOpen = false;
	}

	function handleOverlayClick(event: MouseEvent) {
		if (event.target === event.currentTarget) close();
	}

	function handleKeydown(event: KeyboardEvent) {
		if (event.key === 'Escape') close();
	}
</script>

<svelte:window onkeydown={isOpen ? handleKeydown : null} />

{#if isOpen}
	<div
		class="editor-modal-overlay bg-artid-modal-overlay position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center p-4 z-3"
		role="dialog"
		aria-modal="true"
		tabindex="-1"
		onclick={handleOverlayClick}
		onkeydown={handleKeydown}
		transition:fade={{ duration: 150 }}
	>
		<div
			class="bg-artid-surface border border-2 border-artid-border rounded-3 p-4 position-relative mh-100 overflow-auto"
		>
			<button
				type="button"
				class="btn btn-link text-artid-dark text-decoration-none position-absolute top-0 end-0 m-2 p-1 lh-1"
				aria-label="Chiudi"
				onclick={close}
			>
				<i class="bi bi-x fs-3"></i>
			</button>
			{@render children()}
		</div>
	</div>
{/if}

<style lang="scss">
	.editor-modal-overlay {
		--bs-bg-opacity: 0.75;
		backdrop-filter: blur(0.2rem);
	}
</style>
