<script lang="ts">
	import { page } from "$app/state";

	const user = $derived(page.data.user);
</script>

<h1>Profilo</h1>
<p class="text-muted">Visualizza e modifica le tue informazioni personali.</p>

{#if user}
	<div class="card">
		<div class="card-body">
			<div class="row mb-3">
				<div class="col-md-6">
					<strong>Nome:</strong> {user.name}
				</div>
				<div class="col-md-6">
					<strong>Cognome:</strong> {user.surname}
				</div>
			</div>
			<div class="mb-3">
				<strong>Email:</strong> {user.mail}
			</div>
			{#if user.profession}
				<div class="mb-3">
					<strong>Professione:</strong> {user.profession}
				</div>
			{/if}
			{#if user.biography}
				<div class="mb-3">
					<strong>Biografia:</strong> {user.biography}
				</div>
			{/if}
		</div>
	</div>
{/if}
