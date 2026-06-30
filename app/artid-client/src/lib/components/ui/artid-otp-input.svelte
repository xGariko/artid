<script lang="ts">
	import { onMount } from 'svelte';

	// Input OTP segmentato: N caselle (default 6) che alimentano un unico campo nascosto `name`,
	// così le action server leggono il codice come una stringa singola (come ArtidInput).
	let {
		value = $bindable(''),
		length = 6,
		name,
		error,
		autofocus = false,
		ariaLabel = 'Codice di verifica',
		oncomplete
	}: {
		value?: string;
		length?: number;
		name?: string;
		error?: string;
		autofocus?: boolean;
		ariaLabel?: string;
		// Invocato quando tutte le caselle sono compilate (auto-submit senza pulsante). Rifà solo
		// quando `value` cambia: dopo un errore il codice invariato non re-innesca la verifica.
		oncomplete?: (value: string) => void;
	} = $props();

	let boxes: HTMLInputElement[] = [];

	// Una cifra per casella, derivata dal valore corrente.
	let digits = $derived(Array.from({ length }, (_, i) => value[i] ?? ''));

	function setAt(index: number, digit: string) {
		const next = value.padEnd(index, ' ').split('');
		next[index] = digit;
		value = next.join('').replace(/\s+$/, '').slice(0, length);
	}

	function focusAt(index: number) {
		const box = boxes[Math.max(0, Math.min(index, length - 1))];
		box?.focus();
		box?.select();
	}

	// Distribuisce più cifre (incolla o autofill) a partire da `from`.
	function spread(text: string, from: number) {
		const chars = text.replace(/\D/g, '').slice(0, length - from);
		if (!chars) return;
		const next = value.padEnd(from, ' ').split('');
		for (let i = 0; i < chars.length; i++) next[from + i] = chars[i];
		value = next.join('').replace(/\s+$/, '').slice(0, length);
		focusAt(from + chars.length);
	}

	function onInput(index: number, event: Event) {
		const only = (event.currentTarget as HTMLInputElement).value.replace(/\D/g, '');
		if (!only) {
			setAt(index, '');
			return;
		}
		if (only.length > 1) {
			spread(only, index);
			return;
		}
		setAt(index, only);
		focusAt(index + 1);
	}

	function onKeydown(index: number, event: KeyboardEvent) {
		if (event.key === 'Backspace' && !digits[index]) {
			event.preventDefault();
			setAt(index - 1, '');
			focusAt(index - 1);
		} else if (event.key === 'ArrowLeft') {
			event.preventDefault();
			focusAt(index - 1);
		} else if (event.key === 'ArrowRight') {
			event.preventDefault();
			focusAt(index + 1);
		}
	}

	function onPaste(event: ClipboardEvent) {
		event.preventDefault();
		spread(event.clipboardData?.getData('text') ?? '', 0);
	}

	onMount(() => {
		if (autofocus) focusAt(0);
	});

	// Auto-completamento: notifica il chiamante UNA sola volta per ogni codice completo. `lastFired`
	// (non reattivo) evita che l'effetto, rieseguito ai re-render, reinneschi il submit dello stesso
	// codice — causa del loop. Si azzera quando il codice torna incompleto, così una nuova
	// compilazione (anche identica) torna a innescare la verifica.
	let lastFired = '';
	$effect(() => {
		if (value.length === length) {
			if (value !== lastFired) {
				lastFired = value;
				oncomplete?.(value);
			}
		} else {
			lastFired = '';
		}
	});
</script>

<div class="otp" role="group" aria-label={ariaLabel}>
	{#each digits as digit, i (i)}
		<input
			bind:this={boxes[i]}
			class="otp__box"
			class:is-invalid={!!error}
			class:is-filled={!!digit}
			type="text"
			inputmode="numeric"
			autocomplete={i === 0 ? 'one-time-code' : 'off'}
			maxlength="1"
			value={digit}
			aria-label={`Cifra ${i + 1} di ${length}`}
			oninput={(event) => onInput(i, event)}
			onkeydown={(event) => onKeydown(i, event)}
			onpaste={onPaste}
			onfocus={(event) => event.currentTarget.select()}
		/>
	{/each}
</div>
{#if name}
	<input type="hidden" {name} {value} />
{/if}

<style>
	.otp {
		display: flex;
		gap: clamp(0.35rem, 2.5vw, 0.6rem);
		justify-content: center;
	}

	.otp__box {
		flex: 1 1 0;
		min-width: 0;
		max-width: 3.25rem;
		height: 3.4rem;
		padding: 0;
		text-align: center;
		font-size: 1.5rem;
		font-weight: 600;
		font-variant-numeric: tabular-nums;
		color: var(--artid-text);
		caret-color: var(--artid-primary);
		background: var(--artid-section);
		border: 1.5px solid var(--artid-border);
		border-radius: 0.65rem;
		transition:
			border-color 0.16s ease,
			background-color 0.16s ease;
	}

	.otp__box.is-filled {
		border-color: color-mix(in oklab, var(--artid-primary), transparent 45%);
		background: color-mix(in oklab, var(--artid-primary-subtle), var(--artid-section) 55%);
	}

	.otp__box:focus {
		outline: 2px solid var(--artid-primary);
		outline-offset: 1px;
		border-color: var(--artid-primary);
	}

	.otp__box.is-invalid {
		border-color: var(--bs-danger, #dc3545);
	}
	.otp__box.is-invalid:focus {
		outline-color: var(--bs-danger, #dc3545);
	}
</style>
