<script lang="ts">
	// Definiamo l'interfaccia per il singolo elemento del menu
	interface DropdownItem {
		label: string; // Il testo da mostrare a schermo
		value: string; // Il valore effettivo associato
		icon?: string; // Icona opzionale (es. "✏️", "🗑️")
		isDivider?: boolean; // Se true, mostra una linea di separazione Bootstrap
		isDanger?: boolean; // Se true, applica il testo rosso di Bootstrap
	}

	let {
		btnLabel = 'Seleziona...', // Testo di default sul bottone
		btnStyle = 'primary',
		outline = false,
		icon,
		disabled = false,
		ariaLabel,
		fullWidth = true,
		items = [], // Lista degli elementi passati dall'esterno
		value = $bindable()
	}: {
		btnLabel?: string;
		btnStyle?:
			| 'primary'
			| 'secondary'
			| 'success'
			| 'danger'
			| 'warning'
			| 'info'
			| 'light'
			| 'dark'
			| 'link';
		outline?: boolean;
		icon?: string;
		disabled?: boolean;
		ariaLabel?: string;
		fullWidth?: boolean;
		items: DropdownItem[];
		value: string | null;
	} = $props();

	// Stato locale per l'apertura/chiusura
	let isOpen = $state(false);
	let itemLabel: string | null = $state(null);

	function seleziona(item: DropdownItem) {
		if (item.isDivider) return;

		value = item.value; // Aggiorna la variabile legata in binding con la pagina madre
		itemLabel = item.label;
		isOpen = false; // Chiude il menu
	}
</script>

<div class="dropdown">
	<button
		class="btn btn-{outline ? 'outline-' : ''}{btnStyle} {fullWidth
			? 'w-100'
			: ''} dropdown-toggle px-3"
		type="button"
		aria-expanded={isOpen}
		{disabled}
		aria-label={ariaLabel}
		onclick={() => (isOpen = !isOpen)}
	>
		{items.find((i) => i.value === value)?.label || itemLabel || btnLabel}
	</button>

	<ul
		class="dropdown-menu {isOpen ? 'show' : ''} {fullWidth ? 'w-100' : ''}"
		style="position: absolute; z-index: 100;"
	>
		{#each items as item (item.value)}
			{#if item.isDivider}
				<li><hr class="dropdown-divider" /></li>
			{:else}
				<li>
					<button
						class="dropdown-item {item.isDanger ? 'text-danger' : ''} {value === item.value
							? 'active'
							: ''}"
						type="button"
						onclick={() => seleziona(item)}
					>
						{item.icon ? item.icon + ' ' : ''}{item.label}
					</button>
				</li>
			{/if}
		{/each}
	</ul>
</div>

{#if isOpen}
	<div
		role="presentation"
		style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 99;"
		onclick={() => (isOpen = false)}
	></div>
{/if}
