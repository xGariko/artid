<script lang="ts">
	import type { PublicProfileDetail } from '$lib/api/types';
	import { avatarColorFor, initialsFor } from '$lib/utilities';

	// comment for CI
	let { profile }: { profile: PublicProfileDetail } = $props();

	const fullName = $derived(`${profile.name ?? ''} ${profile.surname ?? ''}`.trim());
	const initials = $derived(initialsFor(profile.name, profile.surname));
	const avatarColor = $derived(avatarColorFor(fullName));

	// linkedinId può essere un handle o un URL completo: normalizziamo verso l'URL del profilo.
	const linkedinUrl = $derived.by(() => {
		const handle = profile.linkedinId?.trim();
		if (!handle) return null;
		return /^https?:\/\//i.test(handle) ? handle : `https://www.linkedin.com/in/${handle}`;
	});
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
	<div class="d-flex align-items-center gap-2">
		{#if linkedinUrl}
			<button
				onclick={()=>{return window.open("" + linkedinUrl, "_blank");}}
				class="btn btn-primary rounded-2 d-flex align-items-center justify-content-center profile-header__linkedin"
				aria-label="Profilo LinkedIn"
			>
				<i class="bi bi-linkedin"></i>
			</button>
		{/if}

		{#if profile.businessEmail}
			<a
				href="mailto:{profile.businessEmail}"
				class="btn btn-primary rounded-2 px-3 py-2 fw-semibold d-flex align-items-center gap-2"
			>
				<i class="bi bi-envelope-fill"></i>
				Contatta
			</a>
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

	.profile-header__linkedin {
		width: 2.75rem;
		height: 2.75rem;
	}
</style>
