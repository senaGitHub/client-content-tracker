<script setup lang="ts">
import { CircleCheckIcon } from '@lucide/vue'
import { computed, useId } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import type { ContentResponse } from '@/api'
import TextLink from '@/components/common/TextLink.vue'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import AttentionItem from './AttentionItem.vue'

const MAX_ITEMS = 5

const props = withDefaults(
  defineProps<{
    title: string
    items?: ContentResponse[]
    loading?: boolean
    emptyText: string
    viewAll: RouteLocationRaw
    showStatus?: boolean
    tone?: 'default' | 'overdue'
  }>(),
  { items: () => [], showStatus: true, tone: 'default' },
)

const headingId = useId()
const visible = computed(() => props.items.slice(0, MAX_ITEMS))
const hidden = computed(() => props.items.length - visible.value.length)
</script>

<template>
  <div :aria-labelledby="headingId" role="group" class="flex min-w-0 flex-col rounded-xl border p-3">
    <div class="flex items-center justify-between gap-2 px-2 pt-1 pb-2">
      <h3 :id="headingId" class="text-sm font-semibold text-foreground">{{ title }}</h3>
      <Skeleton v-if="loading" class="h-5 w-7 rounded-full" />
      <span
        v-else
        :class="cn(
          'rounded-full px-2 py-0.5 text-xs font-medium tabular-nums',
          tone === 'overdue' && items.length > 0 ? 'bg-overdue/10 text-overdue' : 'bg-muted text-muted-foreground',
        )"
      >
        {{ items.length }}
      </span>
    </div>

    <ul v-if="loading" class="flex flex-col" aria-hidden="true">
      <li v-for="n in 3" :key="n" class="flex items-center gap-3 px-2 py-2">
        <Skeleton class="size-8 rounded-lg" />
        <span class="flex flex-1 flex-col gap-1.5">
          <Skeleton class="h-3.5 w-4/5" />
          <Skeleton class="h-3 w-1/2" />
        </span>
      </li>
    </ul>

    <p v-else-if="items.length === 0" class="flex flex-1 items-center gap-2 px-2 py-6 text-sm text-muted-foreground">
      <CircleCheckIcon class="size-4 shrink-0 text-published-dot" aria-hidden="true" />
      {{ emptyText }}
    </p>

    <template v-else>
      <ul class="flex flex-col">
        <li v-for="item in visible" :key="item.id">
          <AttentionItem :item="item" :show-status="showStatus" />
        </li>
      </ul>
      <div v-if="hidden > 0" class="mt-auto px-2 pt-2">
        <TextLink :to="viewAll">View all {{ items.length }}</TextLink>
      </div>
    </template>
  </div>
</template>
