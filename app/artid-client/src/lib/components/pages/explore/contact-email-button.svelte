<script lang="ts">
	import { toast } from '$lib/toast';

	let { email }: { email: string } = $props();

	// "Contatta": il mailto apre il client di posta quando è configurato (comportamento nativo del
	// link). In più, a ogni click copiamo l'indirizzo negli appunti con un feedback: così l'azione
	// "fa sempre qualcosa" anche sui sistemi senza un gestore mailto predefinito (es. desktop senza
	// app di posta), dove il solo mailto non produrrebbe alcun effetto visibile.
	async function copyEmail(): Promise<void> {
		try {
			await navigator.clipboard.writeText(email);
			toast.success(`Indirizzo email copiato: ${email}`);
		} catch {
			// Clipboard non disponibile (contesto non sicuro/permessi negati): mostriamo comunque
			// l'indirizzo così l'utente può scriverselo.
			toast.info(`Scrivi a: ${email}`);
		}
	}
</script>

<a
	href="mailto:{email}"
	onclick={copyEmail}
	class="btn btn-primary rounded-2 px-3 py-2 fw-semibold d-flex align-items-center gap-2"
	aria-label="Email di contatto: {email}"
>
	<i class="bi bi-envelope-fill"></i>
	Contatta
</a>
