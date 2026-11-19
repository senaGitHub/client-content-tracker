<script setup lang="ts">
import type { Component } from 'vue'
import { GlobeIcon, NewspaperIcon, Share2Icon } from '@lucide/vue'
import type { ContentType } from '@/api'
import { TYPE_LABELS } from '@/lib/content'

const props = defineProps<{ type: ContentType }>()

const ICONS: Record<ContentType, Component> = {
  SOCIAL_POST: Share2Icon,
  BLOG_POST: NewspaperIcon,
  WEB_PAGE: GlobeIcon,
}
</script>

<template>
  <span
    class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground"
    :title="TYPE_LABELS[props.type]"
  >
    <component :is="ICONS[props.type]" class="size-4" aria-hidden="true" />
    <span class="sr-only">{{ TYPE_LABELS[props.type] }}</span>
  </span>
</template>
