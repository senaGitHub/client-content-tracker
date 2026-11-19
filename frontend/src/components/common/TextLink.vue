<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
import { ChevronRightIcon } from '@lucide/vue'

defineProps<{ to: RouteLocationRaw }>()
</script>

<template>
  <RouterLink
    :to="to"
    class="inline-flex items-center gap-0.5 rounded-md text-sm font-medium text-primary outline-none hover:underline hover:underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50"
  >
    <slot />
    <ChevronRightIcon class="size-4" aria-hidden="true" />
  </RouterLink>
</template>
