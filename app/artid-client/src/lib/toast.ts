// Wrapper di svelte-sonner: tutti i toast "messaggio" non si auto-chiudono (restano finché l'utente
// non li scarta).
//
// Perché non basta la prop `duration` del <Toaster>: svelte-sonner attiva il caso "durata infinita"
// (nessun timer di chiusura) SOLO quando è il singolo toast ad avere duration === Infinity — il
// default a livello di Toaster NON viene considerato. Con quel default il timer parte comunque con
// setTimeout(Infinity), che JS coercizza a 0, e il toast sparisce all'istante. Per questo iniettiamo
// la durata qui, per-toast, lasciando comunque la possibilità di sovrascriverla per singola chiamata
// (es. `toast.error(msg, { duration: 4000 })`).
import { toast as sonnerToast } from 'svelte-sonner';
import type { ExternalToast } from 'svelte-sonner';

const PERSISTENT_DURATION: ExternalToast = { duration: Number.POSITIVE_INFINITY };

// Antepone la durata infinita ai data del toast: una `duration` passata dal chiamante ha la precedenza.
function persistent(
	method: (message: string, data?: ExternalToast) => string | number
): (message: string, data?: ExternalToast) => string | number {
	return (message, data) => method(message, { ...PERSISTENT_DURATION, ...data });
}

// Drop-in di `toast` di svelte-sonner per gli usi dell'app (success/error/warning/info + message).
// `dismiss` è passthrough (non ha durata). Metodi con ciclo di vita proprio (promise/loading/custom)
// non sono esposti qui: se serviranno, si importano direttamente da svelte-sonner.
export const toast = {
	success: persistent(sonnerToast.success),
	error: persistent(sonnerToast.error),
	warning: persistent(sonnerToast.warning),
	info: persistent(sonnerToast.info),
	message: persistent(sonnerToast.message),
	dismiss: sonnerToast.dismiss
};
