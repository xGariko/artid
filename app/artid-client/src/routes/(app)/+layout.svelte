<script lang="ts">
	import { page } from "$app/state";
	import { enhance } from "$app/forms";

	let { children } = $props();
</script>

<div class="d-flex min-vh-100">
	<nav class="d-flex flex-column bg-dark text-white p-3" style="width: 250px;">
		<a href="/dashboard" class="text-white text-decoration-none fs-4 mb-4 fw-bold">Artid</a>

		<ul class="nav flex-column gap-1">
			<li class="nav-item">
				<a href="/dashboard" class="nav-link text-white">
					<i class="bi bi-house me-2"></i>Dashboard
				</a>
			</li>
			<li class="nav-item">
				<a href="/artids" class="nav-link text-white">
					<i class="bi bi-collection me-2"></i>I miei Artid
				</a>
			</li>
			<li class="nav-item">
				<a href="/profile" class="nav-link text-white">
					<i class="bi bi-person me-2"></i>Profilo
				</a>
			</li>
			<li class="nav-item">
				<a href="/settings" class="nav-link text-white">
					<i class="bi bi-gear me-2"></i>Impostazioni
				</a>
			</li>
		</ul>

		<div class="mt-auto">
			{#if page.data.user}
				<div class="small text-white-50 mb-2">
					{page.data.user.name} {page.data.user.surname}
				</div>
			{/if}
			<form method="POST" action="?/logout" use:enhance>
				<button type="submit" class="btn btn-outline-light btn-sm w-100">
					<i class="bi bi-box-arrow-left me-1"></i>Esci
				</button>
			</form>
		</div>
	</nav>

	<main class="flex-grow-1 p-4 bg-light overflow-auto">
		{@render children()}
	</main>
</div>
