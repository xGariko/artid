<script lang="ts">
	import type { SidebarAction, SidebarButtonGroup } from '$lib/models/sidebar-buttons';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';

	let {
		buttonsGroups,
		sidebarActions,
		activeButton = $bindable(),
		activeTag = $bindable(null)
	}: {
		buttonsGroups: SidebarButtonGroup[];
		sidebarActions: SidebarAction[];
		activeButton: string;
		activeTag?: number | null;
	} = $props();
</script>

<div
	class="bg-artid-section h-100 d-flex flex-column w-15 rounded-3 border border-artid-border sidebar p-3 overflow-y-auto"
>
	{#if sidebarActions.length > 0}
		<div class="mb-4">
			{#each sidebarActions as action, index (index)}
				{#if action.type !== 'tag'}
					<ArtidButton label={action.label} icon={action.icon} onclick={action.callback}
					></ArtidButton>
				{/if}
			{/each}
		</div>
	{/if}

	{#each buttonsGroups as buttonsGroup, index (index)}
		<span class="fw-bold text-artid-text-muted">{buttonsGroup.label}</span>
		<div class="d-flex flex-column justify-content-center align-items-center my-2 mb-4 gap-2">
			{#if buttonsGroup.buttons}
				{#each buttonsGroup.buttons as button, index (index)}
					<button
						class="w-100 border-0 px-3 p-2 rounded-3 bg-artid-section {activeButton === button.value
							? 'active'
							: ''}  text-artid-text sidebar-button"
						onclick={() => (activeButton = button.value)}
					>
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
			{:else if buttonsGroup.tags}
				<div class="w-100 tag-container">
					<div>
						{#each buttonsGroup.tags as tag, index (index)}
							<button
								class="w-100 border-0 d-flex align-items-center text-artid-text {activeTag ===
								tag.id
									? 'tag-active'
									: ''} tag"
								onclick={() => (activeTag = activeTag === tag.id ? null : tag.id)}
							>
								<span
									class="d-block me-2 tag-color"
									style="background-color: #{tag.color ?? '000'};"
								></span>
								<span>{tag.title}</span>
							</button>
						{/each}
					</div>
				</div>
				{#each sidebarActions as action, index (index)}
					{#if action.type === 'tag'}
						<button class="w-100 border-0 new-tag" onclick={action.callback}>
							<span>{action.label}</span>
						</button>
					{/if}
				{/each}
			{/if}
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

	.tag-container {
		overflow-y: auto;
		max-height: 9rem;
		display: flex;
	}

	.tag {
		background-color: transparent;
		&.tag-active {
			color: var(--artid-primary) !important;
			text-decoration: underline;
		}
		.tag-color {
			width: 8px;
			height: 8px;
			border-radius: 100%;
		}

		&:hover {
			text-decoration: underline;
			cursor: pointer;
			color: var(--artid-primary) !important;
		}
	}

	.new-tag {
		text-align: left;
		background-color: transparent;
		text-decoration: underline;
		color: var(--artid-primary) !important;
	}
</style>
