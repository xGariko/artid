<script lang="ts">
	import { page } from "$app/state";

	const user = $derived(page.data.user);
</script>

<h1>Dashboard</h1>
<p class="text-muted">
	Benvenuto{user ? `, ${user.name}` : ""}!
</p>

<div class="row g-3 mt-2">
	<div class="col-md-4">
		<div class="card">
			<div class="card-body">
				<h5 class="card-title">I miei Artid</h5>
				<p class="card-text display-6">0</p>
				<a href="/artids" class="btn btn-outline-primary btn-sm">Vai</a>
			</div>
		</div>
	</div>

	<div class="col-md-4">
		<div class="card">
			<div class="card-body">
				<h5 class="card-title">Condivisioni</h5>
				<p class="card-text display-6">0</p>
			</div>
		</div>
	</div>

	<div class="col-md-4">
		<div class="card">
			<div class="card-body">
				<h5 class="card-title">Notifiche</h5>
				<p class="card-text display-6">0</p>
			</div>
		</div>
	</div>
</div>
