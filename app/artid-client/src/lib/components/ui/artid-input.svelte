<script lang="ts">
	let {
		type = 'text',
		value = $bindable(),
		name,
		label,
		addClass,
		placeholder,
		error,
		inputmode,
		maxlength,
		autocomplete,
		onChange
	}: {
		type?: 'text' | 'number' | 'password' | 'email' | 'tel' | 'date';
		value?: string | number | Date;
		name: string;
		label?: string;
		addClass?: string;
		placeholder?: string;
		error?: string;
		inputmode?: 'text' | 'numeric' | 'tel' | 'email';
		maxlength?: number;
		autocomplete?: 'on' | 'off' | 'one-time-code';
		onChange?: (value: string | number | Date) => void;
	} = $props();
</script>

<div class="form-floating w-100">
	<input
		{type}
		id={name}
		{name}
		bind:value
		{inputmode}
		{maxlength}
		{autocomplete}
		class="form-control {addClass ?? ''} bg-artid-section outline-none rounded-1 {error
			? 'is-invalid'
			: ''}"
		placeholder={placeholder ?? ''}
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
	input:-webkit-autofill{
			background-color: transparent;
	}
</style>
