<script setup lang="ts">
import type { ContentStatus } from '@/api'
import { STATUS_LABELS, STATUS_STYLES } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{ status: ContentStatus; size?: 'sm' | 'md' }>(), { size: 'md' })
</script>

<template>
  <span
    :class="cn(
      'inline-flex shrink-0 items-center gap-1.5 rounded-full font-medium whitespace-nowrap',
      props.size === 'sm' ? 'px-2 py-0.5 text-[11px]' : 'px-2.5 py-0.5 text-xs',
      STATUS_STYLES[props.status].badge,
    )"
  >
    <span :class="cn('size-1.5 rounded-full', STATUS_STYLES[props.status].dot)" aria-hidden="true" />
    {{ STATUS_LABELS[props.status] }}
  </span>
</template>
