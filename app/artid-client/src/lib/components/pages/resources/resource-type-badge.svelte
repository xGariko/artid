<script lang="ts">
	import { getKindMeta, type ResourceKind } from "./resource-types";

	let { kind }: { kind: ResourceKind } = $props();

	const meta = $derived(getKindMeta(kind));
</script>

<span
	class="d-inline-flex align-items-center justify-content-center fw-bold rounded-2 {meta.bgClass} {meta.textClass}"
	style="width: 2.5rem; height: 2.5rem; font-size: 0.7rem;"
	aria-label="Tipo {meta.label}"
>
	{meta.label}
</span>
