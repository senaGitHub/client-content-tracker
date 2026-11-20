<script setup lang="ts">
import { CircleAlertIcon, PlusIcon } from '@lucide/vue'
import { useIntervalFn, useNow } from '@vueuse/core'
import { computed, reactive, ref, watch } from 'vue'
import { dashboardApi } from '@/api'
import type { ClientResponse, ContentResponse, ContentStatus, StatusCounts } from '@/api'
import PageHeader from '@/components/layout/PageHeader.vue'
import ApprovalsPanel from '@/components/overview/ApprovalsPanel.vue'
import BrandStrip from '@/components/overview/BrandStrip.vue'
import ContentFeed from '@/components/overview/ContentFeed.vue'
import KpiTiles from '@/components/overview/KpiTiles.vue'
import { Button } from '@/components/ui/button'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { useDeferredApprove } from '@/composables/useDeferredApprove'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { useStatusChange } from '@/composables/useStatusChange'
import { formatLongDate } from '@/lib/format'

const { openNew, onSaved } = useContentDialog()

// ---- Header ---------------------------------------------------------------------------------

/** No login, so no name: just a greeting that follows the time of day. */
function greetingFor(hour: number): string {
  if (hour >= 5 && hour < 12) return 'Günaydın'
  if (hour >= 12 && hour < 18) return 'İyi günler'
  if (hour >= 18 && hour < 22) return 'İyi akşamlar'
  return 'İyi geceler'
}
// Ticks once a minute so a tab left open keeps the right greeting and date. useIntervalFn is
// tied to this component, so the timer is cleared when the page unmounts.
const now = useNow({ scheduler: (tick) => useIntervalFn(tick, 60_000) })
const greeting = computed(() => greetingFor(now.value.getHours()))
/** "20 Kasım 2026, Cuma" */
const today = computed(() => formatLongDate(now.value))

// ---- Data -----------------------------------------------------------------------------------

const dashboard = useAsyncData((signal) => dashboardApi.get(signal))
const feed = ref<InstanceType<typeof ContentFeed>>()

/** Reloads everything on the page; awaited, so a committed approval stays "published" until fresh data is in. */
async function refreshAll() {
  void refreshNavCounts()
  await Promise.all([dashboard.reload(), feed.value?.refresh()])
}
onSaved(() => void refreshAll())

// ---- Approvals ------------------------------------------------------------------------------

// Onayla: hidden at once, PATCH sent after a few seconds unless "Geri al" cancels it.
const approval = useDeferredApprove({ onCommitted: refreshAll, onFailed: () => void refreshAll() })

// Revizyon: sent at once (with an undo toast), like every other status change.
// Latest known status per item until fresh dashboard data arrives.
const revised = reactive(new Map<number, ContentStatus>())
watch(dashboard.data, () => revised.clear())
const revision = useStatusChange((item: ContentResponse, phase) => {
  revised.set(item.id, item.status)
  if (phase !== 'optimistic') void refreshAll()
})

const approvals = computed(() =>
  dashboard.data.value?.approvals.filter(
    (item) => !approval.isPending(item.id) && (revised.get(item.id) ?? 'IN_REVIEW') === 'IN_REVIEW',
  ),
)

/** Server totals, with approvals that are still waiting to be sent already counted as published. */
const totals = computed<StatusCounts | undefined>(() => {
  const t = dashboard.data.value?.totals
  if (!t) return undefined
  const waiting = approval.pendingItems.value
  const overdue = waiting.filter((item) => item.overdue).length
  return {
    ...t,
    inReview: t.inReview - waiting.length,
    published: t.published + waiting.length,
    overdue: t.overdue - overdue,
  }
})

/** Brand counts, with waiting approvals already moved from Onayda to Yayında (like `totals`). */
const clients = computed<ClientResponse[] | undefined>(() => {
  const list = dashboard.data.value?.clients
  const waiting = approval.pendingItems.value
  if (!list || waiting.length === 0) return list
  return list.map((client) => {
    const mine = waiting.filter((item) => item.client.id === client.id)
    if (mine.length === 0) return client
    const c = client.counts
    return {
      ...client,
      counts: {
        ...c,
        inReview: c.inReview - mine.length,
        published: c.published + mine.length,
        overdue: c.overdue - mine.filter((item) => item.overdue).length,
      },
    }
  })
})

const clientOptions = computed(() =>
  (dashboard.data.value?.clients ?? []).map(({ id, name, coverImageUrl }) => ({ id, name, coverImageUrl })),
)
</script>

<template>
  <div class="flex flex-col gap-5">
    <!-- The page's flex gap does the spacing, so the header's own bottom margin is dropped. -->
    <PageHeader :title="greeting" class="mb-0!">
      <template #description>
        <span class="text-muted-foreground">{{ today }}</span>
      </template>
      <template #actions>
        <Button size="lg" @click="openNew()">
          <PlusIcon :stroke-width="2.4" aria-hidden="true" />
          Yeni İçerik
        </Button>
      </template>
    </PageHeader>

    <div
      v-if="dashboard.error.value && !dashboard.data.value"
      role="alert"
      class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"
    >
      <p class="flex items-start gap-2 text-sm text-muted-foreground">
        <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
        <span><span class="font-medium text-foreground">Özet yüklenemedi.</span> {{ dashboard.error.value }}</span>
      </p>
      <Button variant="outline" :disabled="dashboard.loading.value" @click="dashboard.reload()">Tekrar dene</Button>
    </div>

    <KpiTiles v-else :totals="totals" :metrics="dashboard.data.value?.metrics" />

    <div class="grid grid-cols-1 items-start gap-[18px] min-[1340px]:grid-cols-[minmax(0,1fr)_360px]">
      <div class="flex min-w-0 flex-col gap-[18px]">
        <BrandStrip v-if="!dashboard.error.value || dashboard.data.value" :clients="clients" />
        <ContentFeed
          ref="feed"
          :clients="clientOptions"
          :publishing="approval.pendingItems.value"
          @changed="dashboard.reload()"
        />
      </div>

      <ApprovalsPanel
        v-if="!dashboard.error.value || dashboard.data.value"
        :items="approvals"
        :total="totals?.inReview ?? 0"
        :is-busy="revision.isPending"
        @approve="approval.approve"
        @revise="(item) => revision.move(item, 'DRAFT')"
      />
    </div>
  </div>
</template>
