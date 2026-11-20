<script setup lang="ts">
import { AnchorIcon } from '@lucide/vue'

withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })
</script>

<template>
  <RouterLink
    to="/"
    class="block rounded-xl outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    aria-label="Harbour, Genel Bakış sayfasına git"
  >
    <span class="flex items-center gap-2.5">
      <span
        :class="[
          'flex shrink-0 items-center justify-center bg-linear-140 from-logo-from to-logo-to text-white shadow-[0_4px_12px_color-mix(in_oklab,var(--coral)_30%,transparent)]',
          compact ? 'size-7 rounded-[9px]' : 'size-8 rounded-[10px]',
        ]"
        aria-hidden="true"
      >
        <AnchorIcon :class="compact ? 'size-[15px]' : 'size-[17px]'" :stroke-width="2.2" />
      </span>
      <span class="text-logo text-foreground">Harbour<span class="text-coral">.</span></span>
    </span>
    <span v-if="!compact" class="mt-1 ml-[42px] block text-xs text-secondary-text">Brand &amp; Content Workspace</span>
  </RouterLink>
</template>
