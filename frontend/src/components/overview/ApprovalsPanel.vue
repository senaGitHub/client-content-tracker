<script setup lang="ts">
import { CircleCheckIcon } from '@lucide/vue'
import type { ApprovalItem } from '@/api'
import ApprovalActions from '@/components/approvals/ApprovalActions.vue'
import { Skeleton } from '@/components/ui/skeleton'
import { TooltipProvider } from '@/components/ui/tooltip'
import { useContentDialog } from '@/composables/useContentDialog'
import type { ApprovalActions as Actions } from '@/composables/useApprovalActions'
import { daysUntil, formatShortDate, waitingLabel } from '@/lib/format'

defineProps<{
  /** Undefined while loading. */
  items?: ApprovalItem[]
  /** Everything in review (the list shows only the longest-waiting few). */
  total: number
  /** Onayla / Revizyon / Geri al, shared with the Onay Süreci page. */
  actions: Actions
  /** Items with a move in flight elsewhere on the page (the content feed): their buttons are locked. */
  lockedIds?: number[]
}>()

const { openEdit } = useContentDialog()

/** "Bugün" / "Yarın" when it is that close (as in the feed), otherwise "23 Kas". */
function dueText(dueDate: string): string {
  const days = daysUntil(dueDate)
  if (days === 0) return 'Bugün'
  if (days === 1) return 'Yarın'
  return formatShortDate(dueDate)
}
</script>

<template>
  <section aria-labelledby="overview-approvals">
    <!-- Outside the card, like the other section headings; same height as "Markalara bakış", so on xl the
         two cards start on the same line. -->
    <div class="mb-2.5 flex items-center">
      <h2 id="overview-approvals" class="text-section-title">
        Onay bekleyenler <span v-if="items" class="font-medium text-muted-foreground tabular-nums">({{ total }})</span>
      </h2>
    </div>

    <!-- @container: a narrow panel (under 340px inside, e.g. one of four columns on xl) puts the buttons
         under the cover and title, so the title is not squeezed. -->
    <div class="@container rounded-card border bg-card px-4 pt-3.5 pb-[18px] shadow-card">
      <!-- Two lines, so a narrow panel never breaks a phrase in the middle. -->
      <p class="mb-2 text-xs text-muted-foreground">
        <span class="block">Onayla → yayına alınır</span>
        <span class="block">Revizyon → taslağa döner</span>
      </p>

      <ul v-if="!items" aria-hidden="true">
        <li v-for="n in 4" :key="n" class="flex flex-col gap-2.5 border-t border-divider py-3 @min-[340px]:flex-row @min-[340px]:items-center @min-[340px]:gap-3">
          <div class="flex min-w-0 flex-1 items-start gap-3">
            <Skeleton class="size-[72px] shrink-0 rounded-[9px]" />
            <div class="flex-1 space-y-2">
              <Skeleton class="h-3.5 w-3/4" />
              <Skeleton class="h-3 w-1/2" />
            </div>
          </div>
          <Skeleton class="h-7 w-full rounded-md @min-[340px]:h-14 @min-[340px]:w-20" />
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
          <li
            v-for="item in items"
            :key="item.id"
            class="flex flex-col gap-2.5 border-t border-divider py-3 @min-[340px]:flex-row @min-[340px]:items-center @min-[340px]:gap-3"
          >
            <!-- Cover (72px square, top-aligned with the text) + text stay side by side; on a narrow panel the buttons go under them. -->
            <div class="flex min-w-0 flex-1 items-start gap-3">
              <img
                v-if="item.client.coverImageUrl"
                :src="item.client.coverImageUrl"
                alt=""
                loading="lazy"
                class="size-[72px] shrink-0 rounded-[9px] object-cover"
              />
              <span v-else class="size-[72px] shrink-0 rounded-[9px] bg-muted" aria-hidden="true" />

              <div class="min-w-0 flex-1">
                <!-- Opens the edit sheet, like a title anywhere else. -->
                <button
                  type="button"
                  class="line-clamp-2 rounded-sm text-left text-sm font-bold outline-none hover:text-coral-text focus-visible:ring-3 focus-visible:ring-ring/50"
                  @click="openEdit(item)"
                >
                  {{ item.title }}
                </button>
                <p class="mt-px truncate text-xs text-muted-foreground">{{ item.client.name }}</p>
                <!-- How long it has waited for approval, not how late it is: no red in this panel. -->
                <p class="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1">
                  <span
                    v-if="waitingLabel(item.inReviewSince)"
                    class="inline-block rounded-sm bg-muted px-2 py-0.5 text-xs font-semibold text-secondary-text"
                  >
                    {{ waitingLabel(item.inReviewSince) }}
                  </span>
                  <span class="text-xs text-muted-foreground">Teslim: {{ dueText(item.dueDate) }}</span>
                </p>
              </div>
            </div>

            <ApprovalActions :item="item" :actions="actions" horizontal="when-narrow" :locked="lockedIds?.includes(item.id)" />
          </li>
        </TransitionGroup>
      </TooltipProvider>

      <RouterLink
        to="/approvals"
        class="mt-2.5 block rounded-xl bg-coral-soft p-[11px] text-center text-sm font-semibold text-coral-text outline-none transition-colors hover:bg-nav-active focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        Tüm onaylar →
      </RouterLink>
    </div>
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
