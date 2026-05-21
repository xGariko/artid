<script lang="ts">
	let { label, btnStyle = "primary", outline = false, icon, type = "button", onclick }: {
		label: string,
		btnStyle?: "primary" | "secondary" | "success" | "danger" | "warning" | "info" | "light" | "dark" | "link",
		outline?: boolean,
		icon?: string,
		type?: "button" | "submit" | "reset",
		onclick?: () => void
	} = $props();
</script>

<button
	class="btn btn-{outline ? 'outline-' : ''}{btnStyle} rounded-1 p-2 w-100 fw-semibold"
	onclick={() => onclick?.()}
	type={type}
>
	{#if icon}<i class="bi bi-{icon}"></i>{/if}
	{label}
</button>
