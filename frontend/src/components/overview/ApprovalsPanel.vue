<script setup lang="ts">
import { CheckIcon, CircleCheckIcon, LoaderCircleIcon } from '@lucide/vue'
import type { ContentResponse } from '@/api'
import OverdueChip from '@/components/content/OverdueChip.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { TYPE_LABELS } from '@/lib/content'
import { daysUntil, formatShortDate } from '@/lib/format'

defineProps<{
  /** Undefined while loading. */
  items?: ContentResponse[]
  /** Everything in review (the list shows only the longest-waiting few). */
  total: number
  /** A revision for this item is on its way to the server. */
  isBusy: (id: number) => boolean
}>()
const emit = defineEmits<{ approve: [item: ContentResponse]; revise: [item: ContentResponse] }>()

/** "Bugün" / "Yarın" when it is that close (as in the feed), otherwise "23 Kas". */
function dueText(dueDate: string): string {
  const days = daysUntil(dueDate)
  if (days === 0) return 'Bugün'
  if (days === 1) return 'Yarın'
  return formatShortDate(dueDate)
}

const REVIEW_LINK ={ path: '/content', query: { view: 'list', status: 'IN_REVIEW' } }
</script>

<template>
  <section aria-labelledby="overview-approvals" class="rounded-card border bg-card px-4 py-[18px] shadow-card">
    <h2 id="overview-approvals" class="text-lg font-bold">
      Onay bekleyenler <span v-if="items" class="font-medium text-muted-foreground tabular-nums">({{ total }})</span>
    </h2>
    <p class="mt-1 mb-2 text-xs text-muted-foreground">Onayla → yayına alınır · Revizyon → taslağa döner</p>

    <ul v-if="!items" aria-hidden="true">
      <li v-for="n in 3" :key="n" class="flex items-center gap-3 border-t border-divider py-3">
        <Skeleton class="h-[50px] w-[58px] shrink-0 rounded-[9px]" />
        <div class="flex-1 space-y-2">
          <Skeleton class="h-3.5 w-3/4" />
          <Skeleton class="h-3 w-1/2" />
        </div>
        <Skeleton class="h-14 w-20 rounded-md" />
      </li>
    </ul>

    <div v-else-if="items.length === 0" class="flex flex-col items-center gap-2 border-t border-divider px-4 py-8 text-center">
      <span class="flex size-10 items-center justify-center rounded-full bg-approve-soft text-approve-soft-foreground">
        <CircleCheckIcon class="size-5" aria-hidden="true" />
      </span>
      <p class="text-sm font-semibold">Onay bekleyen içerik yok</p>
      <p class="text-xs text-muted-foreground">Onaya gönderilen içerikler burada sıraya girer.</p>
    </div>

    <TooltipProvider v-else :delay-duration="300">
      <TransitionGroup tag="ul" name="approval" class="relative">
        <li v-for="item in items" :key="item.id" class="flex items-center gap-3 border-t border-divider py-3">
          <img
            v-if="item.client.coverImageUrl"
            :src="item.client.coverImageUrl"
            alt=""
            loading="lazy"
            class="h-[50px] w-[58px] shrink-0 rounded-[9px] object-cover"
          />
          <span v-else class="h-[50px] w-[58px] shrink-0 rounded-[9px] bg-muted" aria-hidden="true" />

          <div class="min-w-0 flex-1">
            <p class="line-clamp-2 text-sm font-bold">{{ item.title }}</p>
            <p class="mt-px truncate text-xs text-muted-foreground">{{ item.client.name }} • {{ TYPE_LABELS[item.type] }}</p>
            <OverdueChip v-if="item.overdue" :due-date="item.dueDate" class="mt-1.5" />
            <span v-else class="mt-1.5 inline-block rounded-sm bg-muted px-2 py-0.5 text-xs font-semibold text-secondary-text">
              Teslim: {{ dueText(item.dueDate) }}
            </span>
          </div>

          <div class="flex shrink-0 flex-col gap-1.5">
            <Tooltip>
              <TooltipTrigger as-child>
                <Button
                  variant="approve"
                  size="xs"
                  class="h-7 rounded-md px-2.5 text-xs"
                  :disabled="isBusy(item.id)"
                  :aria-label="`Onayla: ${item.title}`"
                  @click="emit('approve', item)"
                >
                  <CheckIcon :stroke-width="2.8" />
                  Onayla
                </Button>
              </TooltipTrigger>
              <TooltipContent side="top" align="end">Onayla ve yayına al · geri alınabilir</TooltipContent>
            </Tooltip>
            <Button
              variant="outline"
              size="xs"
              class="h-7 rounded-md px-2.5 text-xs text-secondary-text"
              :disabled="isBusy(item.id)"
              :aria-label="`Revizyona gönder: ${item.title}`"
              @click="emit('revise', item)"
            >
              <LoaderCircleIcon v-if="isBusy(item.id)" class="animate-spin" aria-hidden="true" />
              Revizyon
            </Button>
          </div>
        </li>
      </TransitionGroup>
    </TooltipProvider>

    <RouterLink
      :to="REVIEW_LINK"
      class="mt-2.5 block rounded-xl bg-coral-soft p-[11px] text-center text-sm font-semibold text-coral-text outline-none transition-colors hover:bg-nav-active focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      Tüm onaylar →
    </RouterLink>
  </section>
</template>

<style scoped>
.approval-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.approval-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
.approval-enter-active {
  transition: opacity 0.25s ease;
}
.approval-enter-from {
  opacity: 0;
}
@media (prefers-reduced-motion: reduce) {
  .approval-leave-active,
  .approval-enter-active {
    transition: none;
  }
}
</style>
