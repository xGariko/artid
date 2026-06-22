<script lang="ts">
	import { breadcrumb } from '$lib/stores/breadcrumb';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ArtidLogoIconWhite from '$lib/assets/artid_logo_icon_white.svg';
	import { sanitizeHtml } from '$lib/sanitize';
	import { type ResourceType, resourceTypeFromMime } from '$lib/utilities';
	import ArtidDocumentFileCard from '$lib/components/pages/artid/cards/artid-document-file-card.svelte';
	import ArtidImageFileCard from '$lib/components/pages/artid/cards/artid-image-file-card.svelte';

	// `data` fonde i load degli antenati + quello della pagina:
	//  - `data.artid`   ← +layout.server.ts del livello [id]
	//  - `data.profile` ← +page.server.ts di questa pagina (profilo completo dell'utente loggato)
	let { data } = $props();
	let artid = $derived(data.artid);
	let profile = $derived(data.profile);
	let resources = $derived(data.resources);

	let descriptionHtml = $derived(sanitizeHtml(artid.description));

	$effect(() =>
		breadcrumb.register({
			label: 'Anteprima',
			href: resolve('/(app)/artid/details/[id]/preview', { id: page.params.id! }),
			icon: 'bi-eye'
		})
	);
</script>

<div class="border border-2 border-artid-border rounded-2 bg-artid-section mx-5">
	<div class="bg-artid-dark-subtle w-100 rounded-top-1 p-2 px-3 text-light d-flex">
		<img src={ArtidLogoIconWhite} alt="artid-icon" width="20" />
		<span class="ms-2 fw-semibold">Visualizzazione protetta</span>
		<span class="mx-2">ㆍ</span>
		<span>2026© ArtID</span>
	</div>
	<div class="p-5">
		<div class="mt-3 d-flex align-items-center gap-3">
			{#if profile.propicUrl}
				<img src={profile.propicUrl} alt="" width="128" height="128" class="rounded-circle" />
			{:else}
				<div
					style="width: 128px; height: 128px;"
					class="bg-primary rounded-pill d-flex justify-content-center align-items-center text-white fw-bold"
				>
					{profile.name?.charAt(0).toUpperCase()}
					{profile.surname?.charAt(0).toUpperCase()}
				</div>
			{/if}
			<div>
				<h2>{artid.title}</h2>

				<p class="mb-0 fw-semibold">di {profile.name} {profile.surname}</p>
				{#if profile.profession}
					<p class="text-muted mb-0">{profile.profession}</p>
				{/if}
			</div>
		</div>

		<div class="mt-4">
			{#if descriptionHtml}
				<!-- eslint-disable-next-line svelte/no-at-html-tags -- HTML sanificato da sanitizeHtml ($lib/sanitize) -->
				{@html descriptionHtml}
			{/if}
		</div>

		<h3 class="mt-4 fs-4 fw-semibold">Materiali</h3>
		{#if resources.length === 0}
			<p class="text-muted fw-bold text-center w-100 my-5">
				<i class="bi bi-x-lg"></i>
				Nessun materiale collegato a questo ArtID.
			</p>
		{:else}
			<ul class="list-unstyled d-flex flex-column gap-2">
				{#each resources as resource (resource.id)}
					{@const filetype : ResourceType = resourceTypeFromMime(resource.mimeType)}

					{#if filetype === "document"}
						<ArtidDocumentFileCard {resource}></ArtidDocumentFileCard>
					{:else if filetype === "image"}
						<ArtidImageFileCard {resource}></ArtidImageFileCard>
						{:else}
					{/if}
					<li class="d-flex align-items-center gap-2">
						<i class="bi bi-file-earmark" aria-hidden="true"></i>
						<span>{resource.title}</span>
						{#if resource.fileName}
							<span class="text-muted small">{resource.fileName}</span>
						{/if}
					</li>
				{/each}
			</ul>
		{/if}
	</div>
</div>
