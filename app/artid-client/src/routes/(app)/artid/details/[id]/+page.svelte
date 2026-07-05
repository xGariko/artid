<script lang="ts">
	import { page } from '$app/state';
	import type { PageData } from './$types';
	import ArtidDetailsMaterials from '$lib/components/pages/artid/details/artid-details-materials.svelte';
	import ArtidDetailsTopActions from '$lib/components/pages/artid/details/artid-details-top-actions.svelte';
	import ArtidDetailsInformations from '$lib/components/pages/artid/details/artid-details-informations.svelte';
	import type { ArtidResponse, ResourceResponse, TagResponse } from '$lib/api/types';

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	const id = $derived(page.params.id);

	let { data }: { data: PageData } = $props();

	let artid = $derived<ArtidResponse>(data.artid);
	let draggableMaterials = $derived<ResourceResponse[]>(data.materials ?? []);
	let artidTags = $derived<TagResponse[]>(data.tags ?? []);
</script>

<div class="w-100 h-100 d-flex flex-column align-items-center gap-4 p-5">
	<ArtidDetailsTopActions {artid} artidId={Number(artid.id)} {artidTags} />

	<div
		class="bg-artid-section overflow-hidden w-75 rounded-3 border border-artid-border d-flex justify-content-between"
		style="min-height: 0;"
	>
		<div class="w-50 h-100 overflow-y-auto border-end border-artid-border d-flex flex-column">
			<ArtidDetailsInformations
				{artid}
				artidId={Number(artid.id)}
				isActivelyShared={data.isActivelyShared!}
			/>
		</div>

		<div class="w-50 d-flex flex-column overflow-hidden">
			<ArtidDetailsMaterials artidMaterials={draggableMaterials} artidId={Number(artid.id)} />
		</div>
	</div>
</div>
