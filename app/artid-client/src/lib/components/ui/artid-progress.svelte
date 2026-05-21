<script lang="ts">
	import { loading } from '$lib/stores/spinner-loading';
</script>

{#if $loading}
	<div class="artid-progress" role="progressbar" aria-busy="true" aria-label="Caricamento in corso">
		<div class="artid-progress__bar"></div>
	</div>
{/if}

<style>
	.artid-progress {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		height: 5px;
		background: transparent;
		overflow: hidden;
		z-index: 2000;
		pointer-events: none;
	}

	.artid-progress__bar {
		position: absolute;
		top: 0;
		left: 0;
		height: 100%;
		width: 40%;
		background-color: var(--artid-primary);
		border-radius: 0;
		will-change: transform;
		animation: artid-progress-slide 1.4s cubic-bezier(0.4, 0, 0.2, 1) infinite;
	}

	@keyframes artid-progress-slide {
		0% {
			transform: translateX(-100%);
		}
		100% {
			transform: translateX(250%);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.artid-progress__bar {
			animation-duration: 2.4s;
		}
	}
</style>
