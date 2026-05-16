<script lang="ts">
	import { enhance } from "$app/forms";

	let { form } = $props();
	let loading = $state(false);
</script>

<div class="card shadow-sm">
	<div class="card-body p-4">
		<h2 class="text-center mb-4">Accedi ad Artid</h2>

		{#if form?.error}
			<div class="alert alert-danger">{form.error}</div>
		{/if}

		<form
			method="POST"
			use:enhance={() => {
				loading = true;
				return async ({ update }) => {
					loading = false;
					await update();
				};
			}}
		>
			<div class="mb-3">
				<label for="username" class="form-label">Username</label>
				<input
					id="username"
					name="username"
					type="text"
					class="form-control"
					value={form?.username ?? ""}
					required
				/>
			</div>

			<div class="mb-3">
				<label for="password" class="form-label">Password</label>
				<input
					id="password"
					name="password"
					type="password"
					class="form-control"
					required
				/>
			</div>

			<button type="submit" class="btn btn-primary w-100" disabled={loading}>
				{#if loading}
					<span class="spinner-border spinner-border-sm me-2"></span>
				{/if}
				Accedi
			</button>
		</form>

		<p class="text-center mt-3 mb-0">
			Non hai un account? <a href="/register">Registrati</a>
		</p>
	</div>
</div>
