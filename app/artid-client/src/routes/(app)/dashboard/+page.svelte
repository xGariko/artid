<script lang="ts">
	import { user } from '$lib/stores/auth';
	import ArtidDashboardCard from '$lib/components/dahsboard/artid-dashboard-card.svelte';
</script>

<div class="h-100 d-flex flex-column align-items-center justify-content-center">
	<div class="w-50 h-65 d-flex flex-column justify-content-start">
	<h2 class="mb-3 text-start">Bentornat*, <span class="fw-semibold">{$user?.name}</span></h2>

		<div class="row g-3 mb-auto h-100">
			<div class="col-12 col-lg-8">
				<ArtidDashboardCard
					label="ArtID"
				/>
			</div>
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard />
			</div>
		</div>

		<div class="row g-3 h-100 mt-auto">
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard />
			</div>
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard />
			</div>
			<div class="col-12 col-lg-4">
				<ArtidDashboardCard />
			</div>
		</div>
	</div>
</div>

