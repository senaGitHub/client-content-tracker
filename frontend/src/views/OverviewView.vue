<script setup lang="ts">
import { CircleAlertIcon, PlusIcon, SearchIcon } from '@lucide/vue'
import { useDebounceFn } from '@vueuse/core'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '@/api'
import type { ContentResponse, ContentStatus, StatusCounts } from '@/api'
import ApprovalsPanel from '@/components/overview/ApprovalsPanel.vue'
import BrandStrip from '@/components/overview/BrandStrip.vue'
import ContentFeed from '@/components/overview/ContentFeed.vue'
import KpiTiles from '@/components/overview/KpiTiles.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { useDeferredApprove } from '@/composables/useDeferredApprove'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { useStatusChange } from '@/composables/useStatusChange'
import { formatHeaderDate } from '@/lib/format'

const router = useRouter()
const { openNew, onSaved } = useContentDialog()

// ---- Header ---------------------------------------------------------------------------------

/** No login, so no name: just a greeting that follows the time of day. */
function greetingFor(hour: number): string {
  if (hour >= 5 && hour < 12) return 'Günaydın'
  if (hour >= 12 && hour < 18) return 'İyi günler'
  if (hour >= 18 && hour < 22) return 'İyi akşamlar'
  return 'İyi geceler'
}
const greeting = greetingFor(new Date().getHours())
const today = formatHeaderDate()

// The box filters the feed as you type; Enter opens the full list with the same search.
const search = ref('')
const feedQuery = ref('')
const commitSearch = useDebounceFn((value: string) => (feedQuery.value = value), 300)
watch(search, (value) => void commitSearch(value))

function openSearchInList() {
  const q = search.value.trim()
  void router.push({ path: '/content', query: q ? { view: 'list', q } : { view: 'list' } })
}
function clearSearch() {
  search.value = ''
  feedQuery.value = ''
}

// ---- Data -----------------------------------------------------------------------------------

const dashboard = useAsyncData((signal) => dashboardApi.get(signal))
/** Tells the feed to reload when something outside it changed the data. */
const feedVersion = ref(0)

async function refreshAll() {
  feedVersion.value++
  void refreshNavCounts()
  await dashboard.reload()
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

const clientOptions = computed(() =>
  (dashboard.data.value?.clients ?? []).map(({ id, name, coverImageUrl }) => ({ id, name, coverImageUrl })),
)
</script>

<template>
  <div class="flex flex-col gap-5">
    <header class="flex flex-wrap items-center justify-between gap-4">
      <div class="min-w-0">
        <h1 class="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[30px]">{{ greeting }}</h1>
        <p v-if="totals" class="mt-1 text-base text-secondary-text">
          <template v-if="totals.inReview > 0">Bugün <b class="font-semibold text-foreground">{{ totals.inReview }} içerik</b> onayını bekliyor</template>
          <template v-else>Onay bekleyen içerik yok</template>
          <template v-if="totals.overdue > 0">
            · <span class="font-semibold text-overdue-text">{{ totals.overdue }} içerik gecikti</span>
          </template>
        </p>
        <Skeleton v-else-if="dashboard.loading.value" class="mt-2.5 h-4 w-72 max-w-full" />
      </div>

      <div class="flex w-full flex-wrap items-center gap-3.5 sm:w-auto">
        <label
          role="search"
          class="flex h-10 w-full items-center gap-2.5 rounded-lg border border-input bg-card px-3.5 transition-shadow focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 sm:w-[260px]"
        >
          <SearchIcon class="size-[17px] shrink-0 text-foreground" aria-hidden="true" />
          <span class="sr-only">İçerik ara</span>
          <input
            v-model="search"
            type="search"
            placeholder="İçerik ara…"
            class="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground [&::-webkit-search-cancel-button]:hidden"
            @keydown.enter.prevent="openSearchInList"
            @keydown.esc="clearSearch"
          />
        </label>
        <span class="text-sm text-muted-foreground">{{ today }}</span>
        <Button size="lg" class="ml-auto sm:ml-0" @click="openNew()">
          <PlusIcon :stroke-width="2.4" aria-hidden="true" />
          Yeni İçerik
        </Button>
      </div>
    </header>

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

    <div class="grid items-start gap-[18px] min-[1340px]:grid-cols-[minmax(0,1fr)_360px]">
      <div class="flex min-w-0 flex-col gap-[18px]">
        <BrandStrip v-if="!dashboard.error.value || dashboard.data.value" :clients="dashboard.data.value?.clients" />
        <ContentFeed
          :clients="clientOptions"
          :q="feedQuery"
          :version="feedVersion"
          @changed="dashboard.reload()"
          @clear-search="clearSearch"
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
