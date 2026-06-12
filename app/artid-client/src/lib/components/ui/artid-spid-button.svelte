<script lang="ts">
	import spidIcon from '$lib/assets/spid-ico-circle.svg';

	let { label } : { label: string } = $props();
 </script>

<div class="d-flex justify-content-center">
	<button class="btn btn-primary rounded-0 d-flex w-auto align-items-center justify-content-center gap-2 py-2">
		<img src="{spidIcon}" class="spid-icon" alt="SPID">
		<span class="border-left">{label}</span>
	</button>
</div>



<style>
    .spid-icon {
        width: 2rem;
        padding-right: 0.5rem;
        border-right: 1px solid #127AE2;
    }
</style>
