<script lang="ts">
	import type { components } from "$lib/api/schema";
    import artidImage from "$lib/assets/artid_logo_outline_primary.svg"
	import { formatItalianDate, type ArtIdFilterType } from "$lib/utilities";
	import { resolve } from "$app/paths";


    let {artid, filter}: {artid: components["schemas"]["ArtidResponse"], filter: ArtIdFilterType} = $props();

</script>

<a href={resolve('/(app)/artid/details/[id]', { id: String(artid.id) })} class="rounded-3 border border-artid-border w-100 d-block text-decoration-none">
    <div class="card-image">
        <img src={artidImage} alt={`${artid.title} image`}>
        {#if filter === "mine"}
              <span class="action">
            <i class="bi bi-star fs-6 text-primary"></i>
        </span>
        {:else if filter === "sharedWithMe"}
            <span class="action">
                <i class="bi bi-trash fs-6 text-primary"></i>
            </span>
        {/if}
       
    </div>
    <div class="rounded-bottom-3 border-top border-artid-border p-2 card-description">
        <span>{artid.title}</span>
        <span class="d-block">{formatItalianDate(artid.lastModified)}</span>
    </div>
</a>

<style lang="SCSS">
    .card-image{
        position: relative;
    }

    .card-image img{
        width: 100%;
        object-fit: cover;
    }

    .action{
        position: absolute;
        top: 0; 
        right: 0;
        background-color: white;
        border-radius: 50%;
        padding: 2px;
    }

    .card-description{
        background-color: var(--artid-surface);
    }

</style>