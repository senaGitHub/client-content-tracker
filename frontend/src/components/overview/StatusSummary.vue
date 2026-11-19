<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import type { StatusCounts } from '@/api'
import { Skeleton } from '@/components/ui/skeleton'
import { contentLink } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = defineProps<{ totals?: StatusCounts; loading?: boolean }>()

interface Tile {
  key: string
  label: string
  value: number
  dot: string
  to: RouteLocationRaw
  emphasize?: boolean
}

// Plain counts only: no trends or percentages (no fake metrics).
const tiles = computed<Tile[]>(() => {
  const t = props.totals
  if (!t) return []
  return [
    { key: 'draft', label: 'Draft', value: t.draft, dot: 'bg-draft-dot', to: contentLink({ status: 'DRAFT' }) },
    { key: 'review', label: 'In review', value: t.inReview, dot: 'bg-review-dot', to: contentLink({ status: 'IN_REVIEW' }) },
    { key: 'published', label: 'Published', value: t.published, dot: 'bg-published-dot', to: contentLink({ status: 'PUBLISHED' }) },
    { key: 'overdue', label: 'Overdue', value: t.overdue, dot: 'bg-overdue', to: contentLink({ overdue: true }), emphasize: t.overdue > 0 },
  ]
})
</script>

<template>
  <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
    <template v-if="loading || !totals">
      <div v-for="n in 4" :key="n" class="rounded-xl border p-4">
        <Skeleton class="h-4 w-20" />
        <Skeleton class="mt-3 h-7 w-12" />
      </div>
    </template>
    <RouterLink
      v-for="tile in tiles"
      v-else
      :key="tile.key"
      :to="tile.to"
      class="group rounded-xl border p-4 outline-none transition-colors hover:bg-muted/50 focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <span class="flex items-center gap-2 text-sm text-muted-foreground">
        <span :class="cn('size-2 rounded-full', tile.dot)" aria-hidden="true" />
        {{ tile.label }}
      </span>
      <span
        :class="cn(
          'mt-2 block text-2xl font-semibold tracking-tight tabular-nums',
          tile.emphasize ? 'text-overdue' : 'text-foreground',
        )"
      >
        {{ tile.value }}
      </span>
    </RouterLink>
  </div>
</template>
