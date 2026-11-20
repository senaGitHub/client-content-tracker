<script setup lang="ts">
import type { ContentType } from '@/api'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import { TYPE_LABELS, TYPE_ORDER } from '@/lib/content'
import { cn } from '@/lib/utils'

// Three toggle buttons instead of a select: the type is one click and always visible. Full type names
// (TYPE_LABELS, as everywhere else) on one line: side by side from sm, stacked full width below.
defineProps<{ id: string; invalid?: boolean; describedBy?: string }>()
const model = defineModel<ContentType | ''>({ required: true })
</script>

<template>
  <div :id="id" role="group" aria-label="Tür" :aria-describedby="describedBy" class="flex flex-col gap-1.5 sm:flex-row">
    <button
      v-for="type in TYPE_ORDER"
      :key="type"
      type="button"
      :aria-pressed="model === type"
      :aria-label="TYPE_LABELS[type]"
      :class="cn(
        'flex h-10 w-full items-center justify-center gap-1.5 rounded-lg border px-2.5 text-[13px] whitespace-nowrap sm:w-auto sm:flex-1 outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
        model === type
          ? 'border-coral bg-coral-soft font-semibold text-coral-text ring-1 ring-coral ring-inset'
          : 'border-input bg-card font-medium text-foreground hover:bg-muted',
        invalid && model !== type && 'border-destructive',
      )"
      @click="model = type"
    >
      <ContentTypeIcon :type="type" />
      <span>{{ TYPE_LABELS[type] }}</span>
    </button>
  </div>
</template>
