<script lang="ts">
	import type { SidebarAction, SidebarButtonGroup } from '$lib/models/sidebar-buttons';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';

	let { buttonsGroups, sidebarActions, activeButton = $bindable() }: {
		buttonsGroups: SidebarButtonGroup[],
		sidebarActions: SidebarAction[],
		activeButton: string
	} = $props();
</script>

<div
	class="bg-artid-section h-100 d-flex flex-column w-15 rounded-3 border border-artid-border sidebar p-3 overflow-y-auto">
	{#if sidebarActions.length > 0}
		<div class="mb-4">
			{#each sidebarActions as action, index (index)}
				<ArtidButton
					label={action.label}
					icon={action.icon}
					onclick={action.callback}
				></ArtidButton>
			{/each}
		</div>
	{/if}

	{#each buttonsGroups as buttonsGroup, index (index)}
		<span class="fw-bold text-artid-text-muted">{buttonsGroup.label}</span>
		<div class="d-flex flex-column justify-content-center align-items-center my-2 mb-4 gap-2">
			{#each buttonsGroup.buttons as button, index (index)}
				<button
					class="w-100 border-0 px-3 p-2 rounded-3 bg-artid-section {activeButton === button.value ? 'active' : ''}  text-artid-text sidebar-button"
					onclick={() => activeButton = button.value}>
					<span class="d-flex justify-content-between w-100">
						<span class="d-flex align-items-center gap-2">
							<i class="bi bi-{button.icon} fs-5"></i>
							<span class="fw-medium">{button.label}</span>
						</span>
						<span class="d-flex justify-content-center align-items-center">
							{button.count}
						</span>
					</span>
				</button>
			{/each}
		</div>
	{/each}
</div>


<style>
    .sidebar {
        min-width: 15rem;
    }

    .sidebar-button {
        transition: background-color 0.3s ease-in-out;
    }

    .active {
        background-color: var(--artid-primary-subtle) !important;
        color: var(--artid-primary) !important;
    }

    .sidebar-button:hover:not(.active) {
        background-color: var(--artid-primary-dark-subtle) !important;
    }
</style>