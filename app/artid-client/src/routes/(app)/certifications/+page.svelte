<script lang="ts">
	import CertificationList from "$lib/components/pages/certifications/certifications-list.svelte";
	import CertificationEditor from "$lib/components/pages/certifications/certifications-editor.svelte";
	import type { PageData } from "./$types";

	export interface CertificationResponse {
		id?: number;
		title?: string;
		description?: string;
		isPublic?: boolean;
		public?: boolean;
		extension?: string;
		fileSize?: number;
		createdAt?: string;
		lastModified?: string;
	}

	let { data }: { data: PageData } = $props();

	let editorOpen = $state(false);
	let editingCertification = $state<CertificationResponse | undefined>(undefined);

	function handleNewCertification(): void {
		editingCertification = undefined;
		editorOpen = true;
	}

	function handleEditRequest(certification: CertificationResponse): void {
		editingCertification = certification;
		editorOpen = true;
	}
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center p-5 page-background">
	<CertificationList
		certifications={data.certifications || []}
		onEditRequest={handleEditRequest}
		onNewRequest={handleNewCertification}
	/>
</div>

<CertificationEditor bind:isOpen={editorOpen} certification={editingCertification} />

<style>
    .page-background {
        background-color: #f8fafc;
        min-height: calc(100vh - 120px);
    }
</style>