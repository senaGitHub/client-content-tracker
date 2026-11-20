<script setup lang="ts">
import { CircleAlertIcon, PlusIcon } from '@lucide/vue'
import { useIntervalFn, useNow } from '@vueuse/core'
import { computed, ref } from 'vue'
import { dashboardApi } from '@/api'
import PageHeader from '@/components/layout/PageHeader.vue'
import ApprovalsPanel from '@/components/overview/ApprovalsPanel.vue'
import BrandStrip from '@/components/overview/BrandStrip.vue'
import ContentFeed from '@/components/overview/ContentFeed.vue'
import KpiTiles from '@/components/overview/KpiTiles.vue'
import { Button } from '@/components/ui/button'
import { useApprovalActions } from '@/composables/useApprovalActions'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { refreshNavCounts } from '@/composables/useNavCounts'
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

/**
 * Reloads everything on the page (KPIs, brand cards, approvals, feed, sidebar badge) together.
 * Awaited after a committed approval, so its spinner stays until all of them show the new state.
 */
async function refreshAll() {
  await Promise.all([reloadSummary(), feed.value?.refresh()])
}
/** Everything except the feed: the feed calls this itself after its own status changes. */
function reloadSummary() {
  return Promise.all([refreshNavCounts(), dashboard.reload()])
}
onSaved(() => void refreshAll())

// ---- Approvals ------------------------------------------------------------------------------

// Onayla and Revizyon, both deferred (spinner on the pressed button, "Geri al" in the toast), shared
// with the Onay Süreci page. Nothing on this page changes while a move waits: once the PATCH is
// done, refreshAll (awaited) brings every number in together.
const approvals = useApprovalActions(() => dashboard.data.value?.approvals, refreshAll)

const totals = computed(() => dashboard.data.value?.totals)
/** Ids of approvals waiting to be sent; the feed shows those rows as pending. */
const approvingIds = computed(() => approvals.approvingItems.value.map((item) => item.id))

const clientOptions = computed(() =>
  (dashboard.data.value?.clients ?? []).map(({ id, name, coverImageUrl }) => ({ id, name, coverImageUrl })),
)
</script>

<template>
  <div class="flex flex-col gap-5 xl:gap-6">
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

    <!-- On xl, the same 4 columns and 20px column gap as the KPI row: the left side spans 3, the approvals panel
         sits under the Geciken tile with the same edges. Below xl everything stacks, as before. -->
    <div class="grid grid-cols-1 items-start gap-[18px] xl:grid-cols-4 xl:gap-x-5">
      <div class="flex min-w-0 flex-col gap-[18px] xl:col-span-3 xl:gap-6">
        <BrandStrip v-if="!dashboard.error.value || dashboard.data.value" :clients="dashboard.data.value?.clients" />
        <ContentFeed
          ref="feed"
          :clients="clientOptions"
          :approving-ids="approvingIds"
          :reload-summary="reloadSummary"
        />
      </div>

      <ApprovalsPanel
        v-if="!dashboard.error.value || dashboard.data.value"
        class="min-w-0"
        :items="approvals.visible.value"
        :total="totals?.inReview ?? 0"
        :actions="approvals"
      />
    </div>
  </div>
</template>
