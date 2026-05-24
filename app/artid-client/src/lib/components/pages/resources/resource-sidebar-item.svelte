<script lang="ts">
	// Riga riusabile della sidebar: icona + label + count + active state.
	// Stateless: tutto via prop, l'orchestrazione (cosa è attivo, cosa fa
	// il click) sta nel parent. Così è riutilizzabile in qualsiasi nav list.

	let {
		icon,
		label,
		count,
		active = false,
		onclick
	}: {
		icon: string;
		label: string;
		count: number;
		active?: boolean;
		onclick?: () => void;
	} = $props();
</script>

<button
	type="button"
	{onclick}
	class="btn w-100 d-flex align-items-center gap-2 px-2 py-2 rounded-2 border-0 text-start {active ? 'bg-artid-light text-primary fw-semibold' : 'text-artid-text'}"
	aria-pressed={active}
>
	<i class="bi bi-{icon}" aria-hidden="true"></i>
	<span class="flex-grow-1">{label}</span>
	<span class="small {active ? 'text-primary' : 'text-secondary'}">{count}</span>
</button>
