<script setup lang="ts">
import { CircleAlertIcon, CircleCheckIcon, SearchXIcon } from '@lucide/vue'
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { approvalsApi } from '@/api'
import type { ApprovalSort } from '@/api'
import ApprovalQueueRow from '@/components/approvals/ApprovalQueueRow.vue'
import { QUEUE_COLUMNS } from '@/components/approvals/columns'
import EmptyState from '@/components/common/EmptyState.vue'
import ListPager from '@/components/common/ListPager.vue'
import FilterSelect from '@/components/content/FilterSelect.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { TooltipProvider } from '@/components/ui/tooltip'
import { useApprovalActions } from '@/composables/useApprovalActions'
import { useAsyncData } from '@/composables/useAsyncData'
import { useClientOptions } from '@/composables/useClientOptions'
import { useContentDialog } from '@/composables/useContentDialog'
import { cn } from '@/lib/utils'

const PAGE_SIZE = 10

// ---- URL state (?clientId=&sort=&page=), so the view survives a reload and the Back button ---

const route = useRoute()
const router = useRouter()

/**
 * The Bekleme select. Unset (the "Bekleme Süresi" placeholder) means the server's default order,
 * longest-waiting first; a picked option goes to the URL and the API, even "En uzun".
 */
const WAITING_OPTIONS: { value: ApprovalSort; label: string }[] = [
  { value: 'waiting,desc', label: 'En uzun' },
  { value: 'waiting,asc', label: 'En kısa' },
]

function positiveInt(value: unknown): number | undefined {
  const n = Number(value)
  return Number.isInteger(n) && n > 0 ? n : undefined
}
const clientId = computed(() => positiveInt(route.query.clientId))
/** Only a known value the user picked; anything else in the URL is ignored (default order). */
const sort = computed(() => WAITING_OPTIONS.find((o) => o.value === route.query.sort)?.value)
/** One-based. */
const page = computed(() => positiveInt(route.query.page) ?? 1)

function update(next: { clientId?: number; sort?: ApprovalSort; page?: number }) {
  const query: Record<string, string> = {}
  if (next.clientId) query.clientId = String(next.clientId)
  if (next.sort) query.sort = next.sort
  if (next.page && next.page > 1) query.page = String(next.page)
  void router.replace({ query })
}
/** A brand or order change goes back to page 1. */
function setClient(value: string | undefined) {
  update({ clientId: value ? Number(value) : undefined, sort: sort.value })
}
function setSort(value: string | undefined) {
  update({ clientId: clientId.value, sort: value as ApprovalSort | undefined })
}

const { clients } = useClientOptions()
const clientOptions = computed(() => clients.value.map((c) => ({ value: String(c.id), label: c.name })))

// ---- Data -----------------------------------------------------------------------------------

const { data, error, loading, reload } = useAsyncData((signal) =>
  approvalsApi.list({ clientId: clientId.value, sort: sort.value }, { page: page.value - 1, size: PAGE_SIZE }, signal),
)
watch([clientId, sort, page], () => void reload())

// A page that no longer exists (its last item was approved, or an old link): go to the last real page.
watch(data, (current) => {
  if (current && current.items.length === 0 && current.totalPages > 0 && page.value > current.totalPages) {
    update({ clientId: clientId.value, sort: sort.value, page: current.totalPages })
  }
})

// Same Onayla / Revizyon / Geri al as the Overview panel. After a committed move the list reloads
// (awaited, so the row leaves with fresh data); the sidebar badge is refreshed alongside it.
const actions = useApprovalActions(() => data.value?.items, reload)

// Edits and status changes made in the edit sheet.
const { onSaved } = useContentDialog()
onSaved(() => void reload())

const showSkeleton = computed(() => loading.value && !data.value)
const isEmpty = computed(() => !!data.value && data.value.totalElements === 0)
</script>

<template>
  <div>
    <!-- mb-5 + the filter row's mb-5: the same 20px gaps as the Content page's header, filters and list. -->
    <PageHeader title="Onay Süreci" description="Onay bekleyen içerikleri inceleyin, yayına alın ya da revizyona gönderin." class="mb-5" />

    <!-- Filter row above the table, right-aligned with nothing on the left; on mobile full width, side by side. -->
    <div class="mb-5 grid grid-cols-2 gap-2 sm:flex sm:items-center sm:justify-end">
      <FilterSelect
        :model-value="clientId ? String(clientId) : undefined"
        :options="clientOptions"
        all-label="Tüm Markalar"
        label="Marka"
        size="sm"
        class="sm:max-w-56"
        @update:model-value="setClient"
      />
      <FilterSelect
        :model-value="sort"
        :options="WAITING_OPTIONS"
        all-label="Bekleme Süresi"
        :all-option="false"
        prefix="Bekleme: "
        label="Bekleme süresine göre sırala"
        size="sm"
        @update:model-value="setSort"
      />
    </div>

    <div
      v-if="error && !data"
      role="alert"
      class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"
    >
      <p class="flex items-start gap-2 text-sm text-muted-foreground">
        <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
        <span><span class="font-medium text-foreground">Onaylar yüklenemedi.</span> {{ error }}</span>
      </p>
      <Button variant="outline" :disabled="loading" @click="reload">Tekrar dene</Button>
    </div>

    <EmptyState
      v-else-if="isEmpty && clientId"
      :icon="SearchXIcon"
      title="Bu markada onay bekleyen içerik yok"
      description="Başka bir marka seçin ya da tüm markaları gösterin."
    >
      <Button variant="outline" @click="setClient(undefined)">Tüm markaları göster</Button>
    </EmptyState>

    <EmptyState
      v-else-if="isEmpty"
      :icon="CircleCheckIcon"
      title="Onay bekleyen içerik yok"
      description="Onaya gönderilen içerikler burada sıraya girer."
    />

    <!--
      An ARIA table built from a list: role="row" on the heading row and each item, role="cell" inside
      (ApprovalQueueRow). The headings are desktop only.
    -->
    <section
      v-else
      role="table"
      aria-label="Onay bekleyen içerikler"
      :class="cn('rounded-card border bg-card px-3.5 pt-1 pb-2 shadow-card transition-opacity', loading && data && 'opacity-60')"
      :aria-busy="loading"
    >
      <!-- Column headings, aligned with the row's columns. The order is set in the header selects. -->
      <div role="row" :class="cn('hidden px-2.5 py-3 text-[13px] font-bold text-secondary-text', QUEUE_COLUMNS)">
        <span role="columnheader">İçerik</span>
        <span role="columnheader">Marka</span>
        <span role="columnheader">Bekleme</span>
        <span role="columnheader">Teslim</span>
        <span role="columnheader"><span class="sr-only">İşlemler</span></span>
      </div>

      <ul v-if="showSkeleton" role="none" class="md:border-t md:border-divider" aria-hidden="true">
        <li
          v-for="n in 6"
          :key="n"
          :class="cn('flex flex-col gap-2.5 border-t border-divider px-2.5 py-3 first:border-t-0', QUEUE_COLUMNS)"
        >
          <div class="space-y-2">
            <Skeleton class="h-3.5 w-3/4" />
            <Skeleton class="h-3 w-1/3" />
          </div>
          <Skeleton class="hidden h-3.5 w-3/4 md:block" />
          <Skeleton class="hidden h-5 w-28 max-w-full rounded-sm md:block" />
          <Skeleton class="hidden h-3.5 w-20 max-w-full md:block" />
          <Skeleton class="h-7 w-full rounded-md" />
        </li>
      </ul>

      <TooltipProvider v-else :delay-duration="300">
        <ul role="rowgroup" class="md:border-t md:border-divider">
          <ApprovalQueueRow v-for="item in actions.visible.value" :key="item.id" :item="item" :actions="actions" />
        </ul>
      </TooltipProvider>

      <ListPager
        v-if="data"
        :page="page"
        :page-size="PAGE_SIZE"
        :total="data.totalElements"
        :shown="data.items.length"
        @update:page="(p) => update({ clientId, sort, page: p })"
      />
    </section>
  </div>
</template>
