<script setup lang="ts">
import { CheckIcon, CircleCheckIcon, LoaderCircleIcon } from '@lucide/vue'
import { nextTick } from 'vue'
import type { ApprovalItem, ContentResponse } from '@/api'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { daysUntil, formatShortDate, waitingLabel } from '@/lib/format'

defineProps<{
  /** Undefined while loading. */
  items?: ApprovalItem[]
  /** Everything in review (the list shows only the longest-waiting few). */
  total: number
  /** A revision for this item is on its way to the server. */
  isBusy: (id: number) => boolean
  /** "Onayla" was pressed: the publish is waiting (or being sent). The row stays until fresh data. */
  isApproving: (id: number) => boolean
  /** The waiting publish can still be cancelled (the PATCH has not been sent). */
  canUndo: (id: number) => boolean
}>()
const emit = defineEmits<{
  approve: [item: ContentResponse]
  revise: [item: ContentResponse]
  /** Same as the toast's "Geri al". */
  undo: [item: ContentResponse]
}>()

// Keyboard users keep their place: the pressed button is replaced, so focus moves to its counterpart
// (data-approve / data-undo are only used by this panel).
function focusIn(id: number, action: 'undo' | 'approve') {
  void nextTick(() => document.querySelector<HTMLElement>(`[data-${action}="${id}"]`)?.focus())
}
function approve(item: ContentResponse) {
  emit('approve', item)
  focusIn(item.id, 'undo')
}
function undo(item: ContentResponse) {
  emit('undo', item)
  focusIn(item.id, 'approve')
}

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
    <h2 id="overview-approvals" class="text-section-title">
      Onay bekleyenler <span v-if="items" class="font-medium text-muted-foreground tabular-nums">({{ total }})</span>
    </h2>
    <p class="mt-1 mb-2 text-xs text-muted-foreground">Onayla → yayına alınır · Revizyon → taslağa döner</p>

    <ul v-if="!items" aria-hidden="true">
      <li v-for="n in 4" :key="n" class="flex items-center gap-3 border-t border-divider py-3">
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

          <!--
            Both states share one grid cell, so the row keeps its width when they swap. The hidden one
            is invisible and inert (no focus, not read out).
          -->
          <div class="grid shrink-0">
            <div
              :class="['col-start-1 row-start-1 flex flex-col gap-1.5', isApproving(item.id) && 'invisible']"
              :inert="isApproving(item.id)"
            >
              <Tooltip>
                <TooltipTrigger as-child>
                  <Button
                    variant="approve"
                    size="xs"
                    class="h-7 rounded-md px-2.5 text-xs"
                    :disabled="isBusy(item.id)"
                    :aria-label="`Onayla: ${item.title}`"
                    :data-approve="item.id"
                    @click="approve(item)"
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

            <!-- While the publish waits: the same spinner as StatusMenu's pending state, plus an inline undo. -->
            <div
              :class="['col-start-1 row-start-1 flex flex-col items-stretch gap-1.5', !isApproving(item.id) && 'invisible']"
              :inert="!isApproving(item.id)"
            >
              <span class="flex h-7 items-center justify-center">
                <LoaderCircleIcon class="size-3.5 animate-spin text-muted-foreground" aria-hidden="true" />
              </span>
              <Button
                v-if="canUndo(item.id)"
                variant="outline"
                size="xs"
                class="h-7 rounded-md px-2.5 text-xs text-secondary-text"
                :aria-label="`Geri al: ${item.title}`"
                :data-undo="item.id"
                @click="undo(item)"
              >
                Geri al
              </Button>
            </div>
          </div>

          <!-- Announces the change for screen readers; empty (silent) otherwise. -->
          <span class="sr-only" aria-live="polite">{{ isApproving(item.id) ? `${item.title} yayına alınıyor` : '' }}</span>
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
