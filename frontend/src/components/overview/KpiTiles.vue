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
// One short line per tile; with nothing to say (0 or null) the line is left out.
// "bu hafta" is the API's rolling 7-day window (createdThisWeek / publishedThisWeek).
const thisWeek = (n: number) => (n > 0 ? `+${n} bu hafta` : undefined)
const oldestWaiting = (days: number | null) =>
  days === null ? undefined : days === 0 ? 'hepsi bugün geldi' : `en eskisi ${days} gün`
// Overdue means the due date has passed, so this is at least 1 day when it is not null.
const oldestOverdue = (days: number | null) => (days === null ? undefined : `en eskisi ${days} gün`)

const tiles = computed<Tile[]>(() => {
  const t = props.totals
  const m = props.metrics
  if (!t || !m) return []
  return [
    {
      key: 'total',
      label: 'Toplam içerik',
      value: t.total,
      hint: thisWeek(m.createdThisWeek),
      icon: LayersIcon,
      iconClass: 'bg-coral-soft text-coral',
      to: { path: '/content' },
    },
    {
      key: 'review',
      label: 'Onay bekleyen',
      value: t.inReview,
      hint: oldestWaiting(m.oldestInReviewDays),
      icon: ClockIcon,
      iconClass: 'bg-review text-review-foreground',
      to: listLink({ status: 'IN_REVIEW' }),
    },
    {
      key: 'published',
      label: 'Yayında',
      value: t.published,
      hint: thisWeek(m.publishedThisWeek),
      icon: CircleCheckIcon,
      iconClass: 'bg-approve-soft text-published-dot',
      to: listLink({ status: 'PUBLISHED' }),
    },
    {
      key: 'overdue',
      label: 'Geciken',
      value: t.overdue,
      hint: oldestOverdue(m.oldestOverdueDays),
      icon: CircleAlertIcon,
      // Red only when something is actually overdue; at 0 it is a neutral tile like the others.
      iconClass: t.overdue > 0 ? 'bg-overdue text-white' : 'bg-muted text-secondary-text',
      to: listLink({ overdue: 'true' }),
      alert: t.overdue > 0,
    },
  ]
})
</script>

<template>
  <section aria-label="Özet" class="grid grid-cols-2 gap-3 xl:grid-cols-4">
    <template v-if="tiles.length === 0">
      <div v-for="n in 4" :key="n" class="rounded-card border bg-card px-4 py-3.5 shadow-card" aria-hidden="true">
        <div class="flex items-center gap-2">
          <Skeleton class="size-7 shrink-0 rounded-lg" />
          <Skeleton class="h-3 w-20" />
        </div>
        <Skeleton class="mt-2 h-7 w-12" />
      </div>
    </template>

    <RouterLink
      v-for="tile in tiles"
      v-else
      :key="tile.key"
      :to="tile.to"
      :class="cn(
        'block min-w-0 rounded-card border px-4 py-3.5 shadow-card outline-none transition-[border-color,box-shadow] focus-visible:ring-3 focus-visible:ring-ring/50',
        tile.alert
          ? 'border-overdue-border bg-overdue-soft text-overdue-text hover:border-overdue-text/40'
          : 'bg-card text-foreground hover:border-input',
      )"
    >
      <!-- Row 1: 28px icon square + label, centred. Row 2: number + short line. Both wrap on 2x2 mobile. -->
      <span class="flex items-center gap-2">
        <span :class="cn('flex size-7 shrink-0 items-center justify-center rounded-lg', tile.iconClass)">
          <component :is="tile.icon" class="size-4" :stroke-width="tile.alert ? 2.3 : 1.9" aria-hidden="true" />
        </span>
        <span :class="cn('min-w-0 text-[13px] font-medium break-words', tile.alert ? 'text-overdue-text' : 'text-muted-foreground')">
          {{ tile.label }}
        </span>
      </span>
      <span class="mt-1.5 flex flex-wrap items-baseline gap-x-2">
        <span class="text-kpi tabular-nums">{{ tile.value }}</span>
        <span v-if="tile.hint" :class="cn('text-xs', tile.alert ? 'text-overdue-text' : 'text-muted-foreground')">
          {{ tile.hint }}
        </span>
      </span>
    </RouterLink>
  </section>
</template>
