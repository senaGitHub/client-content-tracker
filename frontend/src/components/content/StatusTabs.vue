<script setup lang="ts">
import type { ContentStatus } from '@/api'
import { cn } from '@/lib/utils'

/**
 * Status tabs ("Tümü (36)", "Taslak (14)", ...) for the Content page, the Overview feed and the
 * brand detail. 32px tall and 14px text, the same as FilterSelect size sm and the toolbar search,
 * so a tabs + filters row lines up. The selected tab is coral and semibold.
 */
defineProps<{
  tabs: { value: ContentStatus | undefined; label: string; count?: number }[]
  /** The selected status; undefined = "Tümü". */
  modelValue: ContentStatus | undefined
}>()
const emit = defineEmits<{ 'update:modelValue': [value: ContentStatus | undefined] }>()
</script>

<template>
  <!-- py-1 / -my-1 leave room for the focus ring inside the horizontal scroller. -->
  <div role="group" aria-label="Durum" class="no-scrollbar -mx-1 -my-1 flex max-w-full gap-1 overflow-x-auto px-1 py-1">
    <button
      v-for="tab in tabs"
      :key="tab.label"
      type="button"
      :aria-pressed="modelValue === tab.value"
      :class="cn(
        'flex h-8 shrink-0 items-center rounded-lg px-3.5 text-sm whitespace-nowrap outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
        modelValue === tab.value ? 'bg-coral-soft font-semibold text-coral-text' : 'font-medium text-secondary-text hover:bg-muted',
      )"
      @click="emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}<span v-if="tab.count !== undefined" class="tabular-nums">&nbsp;({{ tab.count }})</span>
    </button>
  </div>
</template>
