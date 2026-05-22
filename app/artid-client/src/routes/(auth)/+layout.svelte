<script lang="ts">
	import loginRegisterIllustration from '$lib/assets/login_register_illustration_logo.svg';
	import ArtidProgress from '$lib/components/ui/artid-progress.svelte';

	let { children } = $props();
</script>

<ArtidProgress/>
<div class="d-flex vh-100 overflow-hidden bg-artid-section">
	<div class="col-6 d-none d-lg-flex align-items-center justify-content-center px-5">
		<img src="{loginRegisterIllustration}" alt="Welcome Illustration" class="mh-100 mw-100 object-fit-contain">
	</div>
	<div class="col-12 col-lg-6 h-100 overflow-y-auto bg-artid-light d-flex align-items-center justify-content-center">
		<div class="m-5 w-75">
			{@render children()}
		</div>
	</div>
</div>
