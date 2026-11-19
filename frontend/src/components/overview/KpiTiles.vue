<script setup lang="ts">
import type { Component } from 'vue'
import { CircleAlertIcon, CircleCheckIcon, ClockIcon, LayersIcon } from '@lucide/vue'
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import type { DashboardMetrics, StatusCounts } from '@/api'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

const props = defineProps<{ totals?: StatusCounts; metrics?: DashboardMetrics }>()

interface Tile {
  key: string
  label: string
  value: number
  /** Short real-data context next to the number; omitted when there is nothing to say. */
  hint?: string
  icon: Component
  iconClass: string
  to: RouteLocationRaw
  alert?: boolean
}

const listLink = (query: Record<string, string>): RouteLocationRaw => ({ path: '/content', query: { view: 'list', ...query } })
const oldest = (days: number | null) => (days === null ? undefined : `en eskisi ${days} gün`)

const tiles = computed<Tile[]>(() => {
  const t = props.totals
  const m = props.metrics
  if (!t || !m) return []
  return [
    {
      key: 'total',
      label: 'Toplam içerik',
      value: t.total,
      hint: `+${m.createdThisWeek} bu hafta`,
      icon: LayersIcon,
      iconClass: 'bg-coral-soft text-coral',
      to: { path: '/content' },
    },
    {
      key: 'review',
      label: 'Onay bekleyen',
      value: t.inReview,
      hint: oldest(m.oldestInReviewDays),
      icon: ClockIcon,
      iconClass: 'bg-review text-review-foreground',
      to: listLink({ status: 'IN_REVIEW' }),
    },
    {
      key: 'published',
      label: 'Yayında',
      value: t.published,
      hint: `${m.publishedThisWeek} bu hafta · geçen hafta ${m.publishedLastWeek}`,
      icon: CircleCheckIcon,
      iconClass: 'bg-approve-soft text-published-dot',
      to: listLink({ status: 'PUBLISHED' }),
    },
    {
      key: 'overdue',
      label: 'Geciken',
      value: t.overdue,
      hint: oldest(m.oldestOverdueDays),
      icon: CircleAlertIcon,
      iconClass: 'bg-overdue text-white',
      to: listLink({ overdue: 'true' }),
      alert: true,
    },
  ]
})
</script>

<template>
  <section aria-label="Özet" class="grid grid-cols-2 gap-3 xl:grid-cols-4">
    <template v-if="tiles.length === 0">
      <div v-for="n in 4" :key="n" class="flex items-center gap-3 rounded-card border bg-card px-4 py-3.5 shadow-card" aria-hidden="true">
        <Skeleton class="size-10 shrink-0 rounded-xl" />
        <div class="flex-1 space-y-2">
          <Skeleton class="h-3 w-20" />
          <Skeleton class="h-6 w-12" />
        </div>
      </div>
    </template>

    <RouterLink
      v-for="tile in tiles"
      v-else
      :key="tile.key"
      :to="tile.to"
      :class="cn(
        'flex items-center gap-3 rounded-card border px-4 py-3.5 shadow-card outline-none transition-[border-color,box-shadow] focus-visible:ring-3 focus-visible:ring-ring/50',
        tile.alert
          ? 'border-overdue-border bg-overdue-soft text-overdue-text hover:border-overdue-text/40'
          : 'bg-card text-foreground hover:border-input',
      )"
    >
      <span :class="cn('flex size-10 shrink-0 items-center justify-center rounded-xl', tile.iconClass)">
        <component :is="tile.icon" class="size-5" :stroke-width="tile.alert ? 2.3 : 1.9" aria-hidden="true" />
      </span>
      <span class="min-w-0">
        <span :class="cn('block text-[13px] font-medium', tile.alert ? 'text-overdue-text' : 'text-muted-foreground')">
          {{ tile.label }}
        </span>
        <span class="flex flex-wrap items-baseline gap-x-2">
          <span class="text-[26px] leading-tight font-extrabold tracking-[-0.02em] tabular-nums">{{ tile.value }}</span>
          <span v-if="tile.hint" :class="cn('text-xs', tile.alert ? 'text-overdue-text' : 'text-muted-foreground')">
            {{ tile.hint }}
          </span>
        </span>
      </span>
    </RouterLink>
  </section>
</template>
