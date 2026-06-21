<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import { api } from '$lib/api/browser-client';
	import ArtidButton from '$lib/components/ui/artid-button.svelte';
	import ArtidModal from '$lib/components/ui/artid-modal.svelte';
	import { formatFileSize, formatItalianDate, badgeColorForExtension, badgeLabelForExtension } from '$lib/utilities';
	import { toast } from 'svelte-sonner';

	export interface CertificationResponse {
		id?: number;
		title: string;
		description?: string;
		isPublic: boolean;
		extension?: string;
		fileSize?: number;
		createdAt?: string;
		lastModified?: string;
	}

	let {
		certifications,
		onEditRequest,
		onNewRequest
	}: {
		certifications: CertificationResponse[];
		onEditRequest?: (certification: CertificationResponse) => void;
		onNewRequest?: () => void;
	} = $props();

	// Stato degli elementi selezionati tramite checkbox
	let selectedIds = $state<Set<number>>(new Set());
	let showDeleteModal = $state(false);

	// Resetta la selezione se la lista delle certificazioni cambia
	$effect(() => {
		certifications;
		selectedIds = new Set();
	});

	const areAllSelected = $derived(
		certifications.length > 0 &&
		certifications.every((c) => c.id != null && selectedIds.has(c.id))
	);

	const hasSelection = $derived(selectedIds.size > 0);

	function toggleSelection(id: number | undefined): void {
		if (id == null) return;
		const next = new Set(selectedIds);
		if (next.has(id)) {
			next.delete(id);
		} else {
			next.add(id);
		}
		selectedIds = next;
	}

	function toggleAll(): void {
		if (areAllSelected) {
			selectedIds = new Set();
		} else {
			selectedIds = new Set(
				certifications.map((c) => c.id).filter((id): id is number => id != null)
			);
		}
	}

	function downloadSelected(): void {
		for (const id of selectedIds) {
			window.open(`/api/certifications/${id}/file`, '_blank', 'noopener');
		}
	}

	function editSelected(): void {
		if (selectedIds.size !== 1) {
			toast.warning('Seleziona un solo attestato per modificarlo');
			return;
		}
		const [targetId] = selectedIds;
		const target = certifications.find((c) => c.id === targetId);
		if (target) onEditRequest?.(target);
	}

	async function deleteSelected(): Promise<void> {
		showDeleteModal = false;
		try {
			const results = await Promise.all(
				[...selectedIds].map((id) =>
					api.DELETE('/api/certifications/{id}', { params: { path: { id } } })
				)
			);
			await invalidateAll();
			if (results.some((r) => r.error)) {
				toast.error("Errore nell'eliminazione di alcune certificazioni");
			} else {
				toast.success('Certificazioni eliminate con successo');
			}
		} catch {
			toast.error("Errore durante l'eliminazione");
		}
	}
</script>

<div class="card p-4 shadow-sm border-0 bg-white main-certification-card">
	<!-- Intestazione con icona di spunta blu scuro (Coerente con image_698ac0.png) -->
	<div class="d-flex align-items-center gap-2 mb-4">
		<div class="check-badge-container d-flex align-items-center justify-content-center">
			<i class="bi bi-patch-check-fill text-white fs-5"></i>
		</div>
		<h3 class="fs-5 fw-bold text-artid-dark m-0">I tuoi attestati</h3>
	</div>

	<!-- Tabella dei Certificati -->
	<div class="flex-grow-1 overflow-y-auto rounded-3 border border-artid-border table-scroll-container">
		<div class="row g-0 align-items-center px-3 py-2 sticky-header bg-artid-muted border-bottom border-artid-border text-artid-text small fw-semibold text-nowrap text-muted">
			<div class="col-1 d-flex align-items-center">
				<input
					type="checkbox"
					class="form-check-input"
					checked={areAllSelected}
					onchange={toggleAll}
					aria-label="Seleziona tutti"
				/>
			</div>
			<div class="col-4">Nome</div>
			<div class="col-2 text-center">Dimensioni</div>
			<div class="col-2 text-center">Creato</div>
			<div class="col-2 text-center">Modificato</div>
			<div class="col-1 text-center">Visibilità</div>
		</div>

		{#each certifications as cert (cert.id)}
			{@const isSelected = cert.id != null && selectedIds.has(cert.id)}
			<div
				class="row g-0 align-items-center px-3 py-3 border-bottom border-artid-border resource-row"
				class:selected={isSelected}
				role="button"
				tabindex="0"
				onclick={() => toggleSelection(cert.id)}
				onkeydown={(e) => (e.key === ' ' || e.key === 'Enter') && toggleSelection(cert.id)}
			>
				<!-- Selezione e Badge Estensione File (PDF/etc.) -->
				<div class="col-1 d-flex align-items-center gap-2" onclick={(e) => e.stopPropagation()} role="presentation">
					<input
						type="checkbox"
						class="form-check-input flex-shrink-0"
						checked={isSelected}
						onchange={() => toggleSelection(cert.id)}
						aria-label={`Seleziona ${cert.title}`}
					/>
					<span
						class="badge-type fw-bold text-dark text-uppercase fs-xs"
						style:background-color={badgeColorForExtension(cert.extension || 'pdf')}
					>
						{badgeLabelForExtension(cert.extension || 'pdf')}
					</span>
				</div>

				<!-- Titolo dell'attestato -->
				<div class="col-4 pe-3 text-truncate fw-medium text-artid-text" title={cert.title}>
					{cert.title}
				</div>

				<!-- Dimensioni del file -->
				<div class="col-2 text-center text-secondary small">
					{cert.fileSize ? formatFileSize(cert.fileSize) : '781 kb'}
				</div>

				<!-- Data Creazione -->
				<div class="col-2 text-center text-secondary small">
					{cert.createdAt ? formatItalianDate(cert.createdAt) : '7 Mag 2026'}
				</div>

				<!-- Data Modifica -->
				<div class="col-2 text-center text-secondary small">
					{cert.lastModified ? formatItalianDate(cert.lastModified) : '7 Mag 2026'}
				</div>

				<!-- Icona Visibilità (Mappata su isPublic: globo o lucchetto) -->
				<div class="col-1 text-center">
					{#if cert.isPublic}
						<i class="bi bi-globe fs-5 text-secondary" title="Pubblico"></i>
					{:else}
						<i class="bi bi-lock-fill fs-5 text-secondary" title="Privato"></i>
					{/if}
				</div>
			</div>
		{/each}

		{#if certifications.length === 0}
			<div class="text-center text-muted py-5">
				<i class="bi bi-patch-check fs-2 d-block mb-2 text-secondary"></i>
				Nessun attestato caricato
			</div>
		{/if}
	</div>

	<!-- Barra delle Azioni (Allineata a sinistra come in image_698ac0.png) -->
	<div class="d-flex align-items-center gap-2 mt-4">
		<ArtidButton
			label="Nuovo"
			icon="plus-lg"
			fullWidth={false}
			onclick={onNewRequest}
		/>
		<ArtidButton
			label="Scarica"
			icon="download"
			disabled={!hasSelection}
			fullWidth={false}
			onclick={downloadSelected}
		/>
		<ArtidButton
			label="Modifica"
			icon="pencil-square"
			outline={true}
			disabled={selectedIds.size !== 1}
			fullWidth={false}
			onclick={editSelected}
		/>
		<ArtidButton
			icon="trash"
			btnStyle="danger"
			outline={true}
			disabled={!hasSelection}
			fullWidth={false}
			ariaLabel="Elimina selezionati"
			onclick={() => { showDeleteModal = true; }}
		/>
	</div>
</div>

<ArtidModal
	bind:isOpen={showDeleteModal}
	title="Conferma eliminazione"
	onConfirm={deleteSelected}
	message="Una volta eliminata la certificazione non sarà recuperabile."
	btnStyle="danger"
/>

<style>
    .main-certification-card {
        width: 100%;
        max-width: 75rem;
        border-radius: 12px;
    }

    .check-badge-container {
        background-color: #0d4b83; /* Colore blu scuro per il badge della spunta */
        width: 2rem;
        height: 2rem;
        border-radius: 50%;
    }

    .table-scroll-container {
        max-height: 450px;
    }

    .sticky-header {
        position: sticky;
        top: 0;
        z-index: 2;
    }

    .resource-row {
        transition: 0.15s ease all;
        cursor: pointer;
    }

    .resource-row:hover {
        background-color: #f1f5f9;
    }

    .resource-row.selected {
        box-shadow: inset 6px 0px 0px -3px var(--artid-primary, #0d6efd);
        background-color: #f8fafc;
    }

    .badge-type {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 1.8rem;
        flex-shrink: 0;
        border-radius: 4px;
        font-size: 0.75rem;
        letter-spacing: 0.02em;
        background-color: #4de2d6 !important; /* Colore turchese per PDF come in image_698ac0.png */
        color: #0f172a !important;
    }

    .fs-xs {
        font-size: 0.7rem;
    }
</style>