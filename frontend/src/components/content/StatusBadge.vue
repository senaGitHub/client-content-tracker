<script setup lang="ts">
import type { ContentStatus } from '@/api'
import { STATUS_LABELS, STATUS_ORDER, STATUS_STYLES } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    status: ContentStatus
    size?: 'sm' | 'md'
    dot?: boolean
    /**
     * Same width for every status: the badge is as wide as its longest label. All labels are
     * stacked invisibly in one grid cell (hidden from screen readers too), with the real one centred
     * on top, so there is no hand-picked width to keep in sync with the font or the copy.
     */
    uniform?: boolean
  }>(),
  { size: 'md', dot: false, uniform: false },
)
</script>

<template>
  <span
    :class="cn(
      'inline-flex shrink-0 items-center gap-1.5 rounded-full font-semibold whitespace-nowrap',
      props.size === 'sm' ? 'px-2.5 py-0.5 text-xs' : 'px-2.5 py-1 text-[13px]',
      STATUS_STYLES[props.status].badge,
    )"
  >
    <span v-if="props.dot" :class="cn('size-2 rounded-full', STATUS_STYLES[props.status].dot)" aria-hidden="true" />
    <span v-if="props.uniform" class="grid">
      <span v-for="s in STATUS_ORDER" :key="s" class="invisible col-start-1 row-start-1" aria-hidden="true">
        {{ STATUS_LABELS[s] }}
      </span>
      <span class="col-start-1 row-start-1 text-center">{{ STATUS_LABELS[props.status] }}</span>
    </span>
    <template v-else>{{ STATUS_LABELS[props.status] }}</template>
  </span>
</template>
