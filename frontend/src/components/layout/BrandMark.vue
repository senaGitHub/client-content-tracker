<script setup lang="ts">
import { AnchorIcon } from '@lucide/vue'

withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })
</script>

<template>
  <RouterLink
    to="/"
    class="flex items-center gap-3 rounded-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    aria-label="Harbour, go to overview"
  >
    <span
      class="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-primary text-primary-foreground"
      aria-hidden="true"
    >
      <AnchorIcon class="size-[18px]" :stroke-width="2" />
    </span>
    <span class="flex min-w-0 flex-col leading-tight">
      <span class="text-[15px] font-semibold tracking-tight text-foreground">Harbour</span>
      <span v-if="!compact" class="truncate text-xs text-muted-foreground">Client &amp; Content Tracker</span>
    </span>
  </RouterLink>
</template>
