<script lang="ts">
	let {
		type = 'text',
		value = $bindable(),
		name,
		label,
		addClass,
		error,
		onChange
	}: {
		type?: 'text' | 'number' | 'password' | 'email' | 'tel' | 'date';
		value?: string | number | Date;
		name: string;
		label?: string;
		addClass?: string;
		error?: string;
		onChange?: (value: string | number | Date) => void;
	} = $props();
</script>

<div class="form-floating w-100">
	<input
		{type}
		id={name}
		{name}
		bind:value
		class="form-control {addClass ?? ''} bg-artid-section outline-none rounded-1 {error
			? 'is-invalid'
			: ''}"
		placeholder=""
		onchange={(e) => onChange?.(e.currentTarget.value)}
	/>
	{#if label}
		<label for={name} class="text-primary">{label}</label>
	{/if}
</div>
{#if error}
	<div class="text-danger small mt-1 ms-1">{error}</div>
{/if}

<style>
	input:-webkit-autofill,
	input:-webkit-autofill:hover,
	input:-webkit-autofill:focus,
	input:-webkit-autofill:active {
		-webkit-box-shadow: 0 0 0 1000px var(--artid-section) inset !important;
		box-shadow: 0 0 0 1000px var(--artid-section) inset !important;
		-webkit-text-fill-color: var(--artid-text) !important;
		caret-color: var(--artid-text);
		transition: background-color 9999s ease-in-out 0s;
	}
</style>
