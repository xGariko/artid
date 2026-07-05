<script lang="ts">
	let {
		label,
		btnStyle = 'primary',
		outline = false,
		icon,
		type = 'button',
		formaction,
		onclick,
		disabled = false,
		ariaLabel,
		fullWidth = true
	}: {
		label?: string,
		btnStyle?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark' | 'link',
		outline?: boolean,
		icon?: string,
		type?: 'button' | 'submit' | 'reset',
		formaction?: string,
		onclick?: () => void,
		disabled?: boolean,
		ariaLabel?: string,
		fullWidth?: boolean
	} = $props();
</script>

<button
	class="btn btn-{outline ? 'outline-' : ''}{btnStyle} rounded-2 px-3 py-2 {fullWidth ? 'w-100' : ''} fw-semibold"
	onclick={() => onclick?.()}
	type={type}
	{formaction}
	disabled={disabled}
	aria-label={ariaLabel}
>
	<span class="d-flex align-items-center justify-content-center gap-2">
		{#if icon}<i class="bi bi-{icon}"></i>{/if}
		{label}
	</span>
</button>
