<script setup lang="ts">
import { ArrowLeftIcon } from '@lucide/vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import PlaceholderPanel from '@/components/layout/PlaceholderPanel.vue'

defineProps<{ id: number }>()
</script>

<template>
  <RouterLink
    to="/clients"
    class="mb-4 inline-flex items-center gap-1.5 rounded-md text-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
  >
    <ArrowLeftIcon class="size-4" aria-hidden="true" />
    Clients
  </RouterLink>
  <PageHeader :title="`Client #${id}`" />
  <PlaceholderPanel text="Client details and their content are built in a later phase." />
</template>
