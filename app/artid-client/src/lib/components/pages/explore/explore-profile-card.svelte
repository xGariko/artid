<script lang="ts">
	import type { PublicProfile } from '$lib/api/types';
	import { avatarColorFor, initialsFor } from '$lib/utilities';

	let { profile }: { profile: PublicProfile } = $props();

	const fullName = $derived(`${profile.name ?? ''} ${profile.surname ?? ''}`.trim());
	const initials = $derived(initialsFor(profile.name, profile.surname));
	const avatarColor = $derived(avatarColorFor(fullName));
</script>

<div
	class="bg-artid-section border border-artid-border rounded-3 p-3 h-100 d-flex flex-column align-items-center text-center explore-profile-card"
>
	<div
		class="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold fs-4 explore-profile-card__avatar"
		style:background-color={avatarColor}
		aria-hidden="true"
	>
		{initials}
	</div>

	<div class="fw-bold text-artid-text text-truncate w-100 mt-3" title={fullName}>
		{fullName}
	</div>

	{#if profile.profession}
		<div
			class="text-uppercase text-muted small fw-semibold text-truncate w-100"
			title={profile.profession}
		>
			{profile.profession}
		</div>
	{/if}

	<div class="mt-auto pt-3 text-artid-text-muted small">
		{profile.publicArtidCount}
		{profile.publicArtidCount === 1 ? 'ArtID pubblico' : 'ArtID pubblici'}
	</div>
</div>

<style>
	/* Hover di scoperta: tinta brand leggera + bordo primario. Bootstrap non ha utility per lo
	   stato hover di background/bordo, quindi qui un minimo di CSS è l'unica soluzione. */
	.explore-profile-card {
			cursor: pointer;
			user-select: none;
		transition:
			background-color 0.15s ease,
			border-color 0.15s ease;
	}

	.explore-profile-card:hover {
		background-color: var(--artid-primary-subtle) !important;
		border-color: var(--artid-primary) !important;
	}

	/* Avatar a dimensione fissa: nessuna utility Bootstrap per un cerchio in rem. */
	.explore-profile-card__avatar {
		width: 4rem;
		height: 4rem;
		flex-shrink: 0;
	}
</style>
