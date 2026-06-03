<script lang="ts">
	import ProfileForm from '$lib/components/pages/profile/profile-form.svelte';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();
</script>

<div class="w-100 h-100 d-flex align-items-center justify-content-center gap-4 p-5">
	<ProfileForm profile={data.profile} />
</div>
