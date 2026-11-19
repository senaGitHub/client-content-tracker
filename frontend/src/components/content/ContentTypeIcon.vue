<script setup lang="ts">
import type { Component } from 'vue'
import { GlobeIcon, NewspaperIcon, Share2Icon } from '@lucide/vue'
import type { ContentType } from '@/api'
import { TYPE_LABELS } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{ type: ContentType; size?: 'sm' | 'md' }>(), { size: 'md' })

const ICONS: Record<ContentType, Component> = {
  SOCIAL_POST: Share2Icon,
  BLOG_POST: NewspaperIcon,
  WEB_PAGE: GlobeIcon,
}
</script>

<template>
  <span
    :class="cn(
      'flex shrink-0 items-center justify-center bg-muted text-muted-foreground',
      props.size === 'sm' ? 'size-6 rounded-md' : 'size-8 rounded-lg',
    )"
    :title="TYPE_LABELS[props.type]"
  >
    <component :is="ICONS[props.type]" :class="props.size === 'sm' ? 'size-3.5' : 'size-4'" aria-hidden="true" />
    <span class="sr-only">{{ TYPE_LABELS[props.type] }}</span>
  </span>
</template>
