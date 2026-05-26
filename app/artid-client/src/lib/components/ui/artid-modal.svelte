<script lang="ts">
	import {
		Button,
		Modal,
		ModalBody,
		ModalFooter,
		ModalHeader
	} from '@sveltestrap/sveltestrap';


	let {
		isOpen = $bindable(),
		onConfirm,
		title,
		message,
		btnStyle = 'primary',
		confirmLabel = 'Conferma',
		cancelLabel = 'Annulla'
	} : {
		isOpen: boolean,
		onConfirm: () => void,
		title: string
		message: string,
		btnStyle?: 'primary' | 'danger' | 'warning',
		confirmLabel?: string,
		cancelLabel?: string
	} = $props();
</script>

<Modal {isOpen} class="border border-artid-border rounded-3 bg-artid-section">
	<ModalHeader>{title}</ModalHeader>
	<ModalBody>
		{message}
	</ModalBody>
	<ModalFooter>
		<Button color={btnStyle} on:click={onConfirm}>{confirmLabel}</Button>
		<Button color="secondary" on:click={()=>{isOpen=false;}}>{cancelLabel}</Button>
	</ModalFooter>
</Modal>
