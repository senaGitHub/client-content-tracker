<script setup lang="ts">
import { PlusIcon } from '@lucide/vue'
import { computed, watch } from 'vue'
import { contentsApi } from '@/api'
import type { ContentStatus, StatusCounts } from '@/api'
import ContentBoard from '@/components/content/board/ContentBoard.vue'
import ContentToolbar from '@/components/content/ContentToolbar.vue'
import StatusTabs from '@/components/content/StatusTabs.vue'
import ContentList from '@/components/content/list/ContentList.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { Button } from '@/components/ui/button'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { useContentQuery } from '@/composables/useContentQuery'
import type { ContentView } from '@/composables/useContentQuery'
import { STATUS_LABELS, STATUS_ORDER } from '@/lib/content'
import { cn } from '@/lib/utils'

const { state, filters, hasFilters, update, clearFilters } = useContentQuery()
const { openNew, onSaved } = useContentDialog()

const VIEWS: { value: ContentView; label: string }[] = [
  { value: 'list', label: 'Liste' },
  { value: 'board', label: 'Pano' },
]

function setView(view: ContentView) {
  if (view === state.value.view) return
  // Status is a list filter only; the board shows every status as a column.
  update({ view, status: undefined })
}

// ---- Status counts: the list's tabs and the "N sonuç" next to the filters ---------------------

const counts = useAsyncData((signal) => contentsApi.statusCounts(filters.value, signal), { immediate: false })
watch(() => JSON.stringify(filters.value), () => void counts.reload(), { immediate: true })
onSaved(() => void counts.reload())

function countFor(c: StatusCounts, status?: ContentStatus): number {
  if (!status) return c.total
  return { DRAFT: c.draft, IN_REVIEW: c.inReview, PUBLISHED: c.published }[status]
}

const tabs = computed(() =>
  [undefined, ...STATUS_ORDER].map((value) => ({
    value,
    label: value ? STATUS_LABELS[value] : 'Tümü',
    count: counts.data.value ? countFor(counts.data.value, value) : undefined,
  })),
)

const resultCount = computed(() => {
  const c = counts.data.value
  if (!c) return undefined
  return state.value.view === 'list' ? countFor(c, state.value.status) : c.total
})
</script>

<template>
  <PageHeader title="İçerikler" description="Tüm markalarınızın içeriklerini tek yerden yönetin." class="mb-5">
    <template #actions>
      <div role="group" aria-label="Görünüm" class="flex rounded-lg bg-muted p-1">
        <button
          v-for="view in VIEWS"
          :key="view.value"
          type="button"
          :aria-pressed="state.view === view.value"
          :class="cn(
            'rounded-md px-4 py-2 text-sm outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
            state.view === view.value
              ? 'bg-card font-semibold text-foreground shadow-[0_1px_3px_rgba(0,0,0,0.08)]'
              : 'font-medium text-muted-foreground hover:text-foreground',
          )"
          @click="setView(view.value)"
        >
          {{ view.label }}
        </button>
      </div>
      <Button size="lg" @click="openNew({ clientId: state.clientId })">
        <PlusIcon :stroke-width="2.4" aria-hidden="true" />
        Yeni İçerik
      </Button>
    </template>
  </PageHeader>

  <div class="flex flex-col gap-5">
    <!--
      One row, as in the Overview feed: status tabs on the left, search and filters on the right.
      justify-between keeps a lone item at the start, so the filters sit left when they wrap under
      the tabs (narrow screens, where they take the full width) and on the board, which has no tabs.
    -->
    <div class="flex flex-wrap items-center justify-between gap-x-6 gap-y-3">
      <StatusTabs
        v-if="state.view === 'list'"
        :tabs="tabs"
        :model-value="state.status"
        @update:model-value="(value) => update({ status: value })"
      />
      <ContentToolbar
        :query="state"
        :has-filters="hasFilters"
        :result-count="resultCount"
        :spread="state.view === 'board'"
        :class="state.view === 'board' ? 'w-full' : 'w-full lg:w-auto'"
        @update="(patch) => update(patch)"
        @clear="clearFilters"
      />
    </div>

    <ContentList
      v-if="state.view === 'list'"
      :filters="filters"
      :status="state.status"
      :page="state.page"
      :sort="state.sort"
      :has-filters="hasFilters"
      @update="(patch, options) => update(patch, options)"
      @clear="clearFilters"
      @changed="counts.reload()"
    />

    <ContentBoard v-else :filters="filters" :has-filters="hasFilters" @clear="clearFilters" @changed="counts.reload()" />
  </div>
</template>
