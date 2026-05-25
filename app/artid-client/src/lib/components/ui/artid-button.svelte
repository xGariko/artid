<script lang="ts">
	let { label, btnStyle = 'primary', outline = false, icon, type = 'button', onclick }: {
		label: string,
		btnStyle?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark' | 'link',
		outline?: boolean,
		icon?: string,
		type?: 'button' | 'submit' | 'reset',
		onclick?: () => void
	} = $props();
</script>

<button
	class="btn btn-{outline ? 'outline-' : ''}{btnStyle} rounded-2 p-1 w-100 fw-semibold"
	onclick={() => onclick?.()}
	type={type}
>
	<span class="d-flex align-items-center justify-content-center">
		{#if icon}<i class="bi bi-{icon} fs-3"></i>{/if}
		{label}
	</span>
</button>
