<script lang="ts">
	import { user } from '$lib/stores/auth';
	import ArtidLogoOutlinePrimary from '$lib/assets/artid_logo_outline_primary.svg';
	import ArtidDashboardCard from '$lib/components/dahsboard/artid-dashboard-card.svelte';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<div class="h-100 d-flex flex-column align-items-center justify-content-center">
	<div class="w-50 h-65 d-flex flex-column justify-content-start">
		<h1 class="mb-3 text-start">Bentornato/a, <span class="fw-bolder">{$user?.name}</span></h1>

		<div class="row g-3 mb-auto h-100">
			<div class="col-12 col-lg-8">
				<ArtidDashboardCard
					label="ArtID"
					customIcon={ArtidLogoOutlinePrimary}
					subLabel="ArtID creati"
					subLabelData={data.artidCount}
					destinationRoute="/artid"
				/>
			</div>
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard
					icon="folder"
					label="Materiali"
					subLabel="Materiali caricati"
					subLabelData={data.resourceCount}
					destinationRoute="/resources"
				/>
			</div>
		</div>

		<div class="row g-3 h-100 mt-auto">
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard
					icon="patch-check"
					label="Certificazioni"
					subLabel="Certificazioni caricate"
					subLabelData={data.certificationCount}
					destinationRoute="/certifications"
				/>
			</div>
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard
					icon="person"
					label="Profilo"
					subLabel="completato"
					subLabelData={`${data.profileCompletion}%`}
					subLabelColor="warning"
					destinationRoute="/profile"
				/>
			</div>
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard
					icon="share"
					label="Condivisioni"
					subLabel="Condivisioni create"
					subLabelData={data.shareCount}
					destinationRoute="/shares"
				/>
			</div>
		</div>
	</div>
</div>

