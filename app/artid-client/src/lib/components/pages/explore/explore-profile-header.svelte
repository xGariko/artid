<script lang="ts">
	import type { PublicProfileDetail } from '$lib/api/types';
	import { avatarColorFor, initialsFor, socialUrlFor } from '$lib/utilities';
	import ContactEmailButton from '$lib/components/pages/explore/contact-email-button.svelte';

	// comment for CI
	let { profile }: { profile: PublicProfileDetail } = $props();

	const fullName = $derived(`${profile.name ?? ''} ${profile.surname ?? ''}`.trim());
	const initials = $derived(initialsFor(profile.name, profile.surname));
	const avatarColor = $derived(avatarColorFor(fullName));

	// Handle/URL social normalizzati verso l'URL completo del profilo (null = niente bottone).
	const linkedinUrl = $derived(socialUrlFor('linkedin', profile.linkedinId));
	const facebookUrl = $derived(socialUrlFor('facebook', profile.facebookId));
	const instagramUrl = $derived(socialUrlFor('instagram', profile.instagramId));
</script>

<div class="d-flex align-items-start justify-content-between gap-3 flex-wrap">
	<div class="d-flex align-items-center gap-3">
		{#if profile.avatarUrl}
			<img
				src={profile.avatarUrl}
				alt={fullName}
				class="rounded-circle profile-header__avatar"
			/>
		{:else}
			<div
				class="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold fs-3 profile-header__avatar"
				style:background-color={avatarColor}
				aria-hidden="true"
			>
				{initials}
			</div>
		{/if}

		<div>
			<div class="d-flex align-items-center gap-2">
				<span class="fs-4 fw-bold text-artid-text">{fullName}</span>
				{#if profile.verified}
					<i
						class="bi bi-patch-check-fill text-primary fs-5"
						title="Identità verificata via SPID"
						aria-label="Identità verificata"
					></i>
				{/if}
			</div>

			{#if profile.profession}
				<div class="text-uppercase text-muted small fw-semibold">{profile.profession}</div>
			{/if}

			{#if profile.location}
				<div class="text-muted small">{profile.location}</div>
			{/if}
		</div>
	</div>
	<div class="d-flex align-items-center gap-2 flex-wrap">
		{#if linkedinUrl}
			<a
				href={linkedinUrl}
				target="_blank"
				rel="noopener"
				class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center profile-header__social"
				aria-label="Profilo LinkedIn"
			>
				<i class="bi bi-linkedin"></i>
			</a>
		{/if}

		{#if facebookUrl}
			<a
				href={facebookUrl}
				target="_blank"
				rel="noopener"
				class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center profile-header__social"
				aria-label="Profilo Facebook"
			>
				<i class="bi bi-facebook"></i>
			</a>
		{/if}

		{#if instagramUrl}
			<a
				href={instagramUrl}
				target="_blank"
				rel="noopener"
				class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center profile-header__social"
				aria-label="Profilo Instagram"
			>
				<i class="bi bi-instagram"></i>
			</a>
		{/if}

		{#if profile.businessEmail}
			<ContactEmailButton email={profile.businessEmail} />
		{/if}
	</div>
</div>

<style>
	.profile-header__avatar {
		width: 5rem;
		height: 5rem;
		flex-shrink: 0;
		object-fit: cover;
	}

	.profile-header__social {
		width: 2.75rem;
		height: 2.75rem;
	}
</style>
