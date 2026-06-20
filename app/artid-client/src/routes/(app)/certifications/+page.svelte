<script lang="ts">
	import ResourceEditor from "$lib/components/pages/resources/resource-editor.svelte";
	import ResourcesList from "$lib/components/pages/resources/resources-list.svelte";
	import type { CertificationResponse } from "$lib/api/types";
	import type { PageData } from "./$types";

	let { data }: { data: PageData } = $props();

	// Editor condiviso tra "Nuovo" (sidebar) e "Modifica" (list): undefined = add mode.
	let editorOpen = $state(false);
	let editingCertification = $state<CertificationResponse | undefined>(undefined);

	function handleNewCertification(): void {
		editingCertification = undefined;
		editorOpen = true;
	}

	function handleEditRequest(resource: CertificationResponse): void {
		editingCertification = resource;
		editorOpen = true;
	}
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center gap-4 p-5">
	<ResourcesList resources={visibleCertification} onEditRequest={handleEditRequest} />
</div>

<ResourceEditor bind:isOpen={editorOpen} resource={editingCertification} certifications={data.certifications}/>
