<script setup lang="ts">
import type { Component, HTMLAttributes } from 'vue'
import { FileTextIcon, GlobeIcon, HeartIcon } from '@lucide/vue'
import type { ContentType } from '@/api'
import { TYPE_LABELS } from '@/lib/content'
import { cn } from '@/lib/utils'

// Inline type marker as in the mockups: a 16px icon, optionally followed by its label.
// Each type has its own icon colour; the label keeps the text colour it is given.
const props = withDefaults(
  defineProps<{ type: ContentType; withLabel?: boolean; class?: HTMLAttributes['class'] }>(),
  { withLabel: false },
)

// Social posts use the heart ("like"): it belongs to no single platform (a post can go to
// Instagram, LinkedIn, TikTok and others) and still reads as social media.
const ICONS: Record<ContentType, Component> = {
  SOCIAL_POST: HeartIcon,
  BLOG_POST: FileTextIcon,
  WEB_PAGE: GlobeIcon,
}

// Icon colours only. Their hues stay clear of the status colours, overdue red and the coral accent.
const ICON_COLOURS: Record<ContentType, string> = {
  SOCIAL_POST: 'text-type-social',
  BLOG_POST: 'text-type-blog',
  WEB_PAGE: 'text-type-web',
}
</script>

<template>
  <span
    :class="cn('inline-flex shrink-0 items-center gap-1.5 text-xs text-secondary-text', props.class)"
    :title="props.withLabel ? undefined : TYPE_LABELS[props.type]"
  >
    <component
      :is="ICONS[props.type]"
      :class="cn('size-4 shrink-0', ICON_COLOURS[props.type])"
      :stroke-width="1.8"
      aria-hidden="true"
    />
    <span :class="props.withLabel ? 'truncate' : 'sr-only'">{{ TYPE_LABELS[props.type] }}</span>
  </span>
</template>
