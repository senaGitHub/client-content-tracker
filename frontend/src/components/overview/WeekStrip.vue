<script setup lang="ts">
import { format } from 'date-fns'
import type { WeekDay } from '@/api'
import PageSection from '@/components/common/PageSection.vue'
import { Skeleton } from '@/components/ui/skeleton'
import { clientLink, STATUS_LABELS, STATUS_STYLES } from '@/lib/content'
import { isTodayDay, parseDay } from '@/lib/format'
import { cn } from '@/lib/utils'

defineProps<{ days?: WeekDay[]; loading?: boolean }>()

const MAX_PER_DAY = 4
</script>

<template>
  <PageSection title="This week" description="Everything due today and over the next six days.">
    <!-- Stacked rows on small screens, seven columns on wide ones; never a horizontal scroll. -->
    <ol v-if="loading || !days" class="grid gap-2 xl:grid-cols-7" aria-hidden="true">
      <li v-for="n in 7" :key="n" class="flex gap-3 rounded-xl border p-3 xl:min-h-36 xl:flex-col">
        <Skeleton class="h-9 w-12 shrink-0" />
        <span class="flex flex-1 flex-col gap-2">
          <Skeleton class="h-3.5 w-full" />
          <Skeleton class="h-3.5 w-2/3" />
        </span>
      </li>
    </ol>

    <ol v-else class="grid gap-2 xl:grid-cols-7">
      <li
        v-for="day in days"
        :key="day.date"
        :class="cn(
          'flex min-w-0 gap-3 rounded-xl border p-3 xl:min-h-36 xl:flex-col xl:gap-2',
          isTodayDay(day.date) && 'border-primary/40 bg-primary/5',
        )"
      >
        <div class="w-12 shrink-0 xl:w-auto">
          <time :datetime="day.date" class="flex flex-col leading-tight xl:flex-row xl:items-baseline xl:gap-1.5">
            <span
              :class="cn(
                'text-xs font-medium uppercase tracking-wide',
                isTodayDay(day.date) ? 'text-primary' : 'text-muted-foreground',
              )"
            >
              {{ isTodayDay(day.date) ? 'Today' : format(parseDay(day.date), 'EEE') }}
            </span>
            <span class="text-lg font-semibold tabular-nums text-foreground xl:text-sm xl:font-medium xl:text-muted-foreground">
              {{ format(parseDay(day.date), 'd') }}
            </span>
          </time>
        </div>

        <p v-if="day.items.length === 0" class="self-center text-sm text-muted-foreground/70 xl:self-start xl:text-xs">
          Nothing due
        </p>
        <ul v-else class="flex min-w-0 flex-1 flex-col gap-1">
          <li v-for="item in day.items.slice(0, MAX_PER_DAY)" :key="item.id">
            <RouterLink
              :to="clientLink(item.client.id)"
              :title="`${item.title} · ${item.client.name} · ${STATUS_LABELS[item.status]}`"
              class="flex min-w-0 items-center gap-2 rounded-md px-1.5 py-1 text-sm outline-none transition-colors hover:bg-muted/60 focus-visible:ring-3 focus-visible:ring-ring/50 xl:text-xs"
            >
              <span :class="cn('size-1.5 shrink-0 rounded-full', STATUS_STYLES[item.status].dot)" aria-hidden="true" />
              <span
                :class="cn(
                  'truncate',
                  item.status === 'PUBLISHED' ? 'text-muted-foreground line-through decoration-muted-foreground/40' : 'text-foreground',
                )"
              >
                {{ item.title }}
              </span>
              <span class="sr-only">, {{ STATUS_LABELS[item.status] }}</span>
            </RouterLink>
          </li>
          <li v-if="day.items.length > MAX_PER_DAY" class="px-1.5 text-xs text-muted-foreground">
            +{{ day.items.length - MAX_PER_DAY }} more
          </li>
        </ul>
      </li>
    </ol>
  </PageSection>
</template>
