<script setup lang="ts">
import type { Component } from 'vue'
import { FileTextIcon, GlobeIcon } from '@lucide/vue'
import type { ContentType } from '@/api'
import { TYPE_LABELS } from '@/lib/content'

// Inline type marker as in the mockups: a 16px icon, optionally followed by its label.
// Social posts get their own colour; the others use the secondary text colour.
const props = withDefaults(defineProps<{ type: ContentType; withLabel?: boolean }>(), { withLabel: false })

// Lucide 1.x has no brand icons, so the social icon is the mockups' rounded-square + lens glyph.
const ICONS: Partial<Record<ContentType, Component>> = {
  BLOG_POST: FileTextIcon,
  WEB_PAGE: GlobeIcon,
}
</script>

<template>
  <span
    class="inline-flex shrink-0 items-center gap-1.5 text-xs text-secondary-text"
    :title="props.withLabel ? undefined : TYPE_LABELS[props.type]"
  >
    <svg
      v-if="props.type === 'SOCIAL_POST'"
      class="size-4 shrink-0 text-type-social"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      aria-hidden="true"
    >
      <rect x="3.5" y="3.5" width="17" height="17" rx="5" />
      <circle cx="12" cy="12" r="4" />
    </svg>
    <component :is="ICONS[props.type]" v-else class="size-4 shrink-0" :stroke-width="1.8" aria-hidden="true" />
    <span :class="props.withLabel ? 'truncate' : 'sr-only'">{{ TYPE_LABELS[props.type] }}</span>
  </span>
</template>
