<script setup lang="ts">
import { CircleAlertIcon, FileTextIcon, SearchXIcon } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { contentsApi } from '@/api'
import type { ClientRef, ContentFilters, ContentResponse, ContentStatus, ContentType, StatusCounts } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import FilterSelect from '@/components/content/FilterSelect.vue'
import StatusBadge from '@/components/content/StatusBadge.vue'
import StatusMenu from '@/components/content/StatusMenu.vue'
import StatusTabs from '@/components/content/StatusTabs.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import type { DueFilter } from '@/composables/useContentQuery'
import { useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { vHighlight } from '@/composables/useHighlight'
import { DUE_OPTIONS, STATUS_ORDER, STATUS_LABELS, TYPE_LABELS, TYPE_ORDER } from '@/lib/content'
import { dueHint, formatShortDate } from '@/lib/format'
import { cn } from '@/lib/utils'

const props = defineProps<{
  /** Brands for the filter (from the dashboard response, so no extra request). */
  clients: ClientRef[]
  /**
   * Items with a move waiting or in flight elsewhere on the page (the approvals panel, any target):
   * their rows show as pending and their menu is locked, so no second PATCH is sent.
   */
  pendingIds: number[]
  /**
   * Reloads the rest of the page (KPIs, brands, approvals, sidebar badge). Called and awaited with
   * the feed's own reload after a status change from this feed, so everything updates together.
   */
  reloadSummary: () => Promise<unknown>
}>()

const ROWS = 6
const TYPE_OPTIONS = TYPE_ORDER.map((t) => ({ value: t, label: TYPE_LABELS[t] }))

const status = ref<ContentStatus>()
const clientId = ref<number>()
const type = ref<ContentType>()
/** The Content page's "Teslim" filter, with the same API parameters (overdue / dueThisWeek). */
const due = ref<DueFilter>('all')

const clientOptions = computed(() => props.clients.map((c) => ({ value: String(c.id), label: c.name })))

const filters = computed<ContentFilters>(() => ({
  clientId: clientId.value,
  type: type.value,
  overdue: due.value === 'overdue',
  dueThisWeek: due.value === 'week',
}))
const hasFilters = computed(() => !!(clientId.value || type.value || status.value || due.value !== 'all'))

const { openNew, openEdit } = useContentDialog()

// Rows: what needs attention first (overdue, then nearest deadlines; published last). See ?sort=priority.
const SORT = 'priority'
const rows = useAsyncData(
  (signal) => contentsApi.list({ ...filters.value, status: status.value }, { size: ROWS, sort: SORT }, signal),
  { immediate: false },
)
// Tab counts follow the brand / type / due filters, never the selected status.
const counts = useAsyncData((signal) => contentsApi.statusCounts(filters.value, signal), { immediate: false })

watch(() => JSON.stringify([filters.value, status.value]), () => void rows.reload(), { immediate: true })
watch(() => JSON.stringify(filters.value), () => void counts.reload(), { immediate: true })
/**
 * Reloads rows and tab counts; the page awaits it after something outside the feed changed the
 * data (approvals, dialog saves), so a pending row only changes once fresh rows are in.
 */
async function refresh() {
  await Promise.all([rows.reload(), counts.reload()])
}

// Tab counts are the server's: a publish that is still waiting changes nothing until it is done.
const tabs = computed(() => {
  const c: StatusCounts | undefined = counts.data.value
  return [
    { value: undefined, label: 'Tümü', count: c?.total },
    ...STATUS_ORDER.map((s) => ({
      value: s,
      label: STATUS_LABELS[s],
      count: c ? { DRAFT: c.draft, IN_REVIEW: c.inReview, PUBLISHED: c.published }[s] : undefined,
    })),
  ]
})

// ---- Inline status changes (rows keep their place, like the content list) ---------------------

// "Yayına al" waits ~5 s with "Geri al" (see useStatusChange); other moves go at once.
// - A waiting publish is not shown as published: the row keeps its status and StatusMenu shows its
//   pending state (badge + spinner), like the rest of the Overview, where nothing changes until done.
// - Other moves show their new status right away, as before.
// - Once the server has answered, the feed and the rest of the page reload together. The promise is
//   awaited for a committed publish, so the row stays pending until the fresh data is in.
function place(item: ContentResponse, phase: PlacePhase): Promise<unknown> | void {
  if (phase === 'optimistic') {
    if (item.status === 'PUBLISHED') return
    const page = rows.data.value
    if (page) rows.data.value = { ...page, items: page.items.map((row) => (row.id === item.id ? item : row)) }
    return
  }
  return Promise.all([refresh(), props.reloadSummary()])
}
const { move, isPending, pendingIds: ownPendingIds } = useStatusChange(place)

/** Pending from this feed's own menus, or from the page's approvals panel. */
const rowPending = (id: number) => isPending(id) || props.pendingIds.includes(id)

// This feed's own moves in flight, so the page can lock the same item in the approvals panel.
defineExpose({ refresh, ownPendingIds })

// ---- Display helpers ------------------------------------------------------------------------

const allLink = computed<RouteLocationRaw>(() => {
  const query: Record<string, string> = { view: 'list', sort: SORT }
  if (status.value) query.status = status.value
  if (clientId.value) query.clientId = String(clientId.value)
  if (type.value) query.type = type.value
  // Same query names the Content page reads (useContentQuery).
  if (due.value === 'overdue') query.overdue = 'true'
  if (due.value === 'week') query.dueThisWeek = 'true'
  return { path: '/content', query }
})

function clearFilters() {
  status.value = undefined
  clientId.value = undefined
  type.value = undefined
  due.value = 'all'
}

const items = computed(() => rows.data.value?.items ?? [])
const showSkeleton = computed(() => rows.loading.value && !rows.data.value)
const isEmpty = computed(() => !!rows.data.value && rows.data.value.totalElements === 0)
</script>

<template>
  <section aria-labelledby="overview-feed">
    <!-- Its own line, like the other section headings. -->
    <h2 id="overview-feed" class="mb-2.5 text-section-title">İçerik akışı</h2>

    <!-- Tabs left, filters right; when they do not fit, the filters wrap to the next line, left-aligned
         (justify-between keeps a lone item at the start). -->
    <div class="mb-2.5 flex flex-wrap items-center justify-between gap-3">
      <StatusTabs :tabs="tabs" :model-value="status" @update:model-value="(value) => (status = value)" />

      <!-- Compact filters (FilterSelect size sm), as wide as their text. -->
      <div class="flex w-full flex-wrap gap-2 sm:w-auto">
        <FilterSelect
          :model-value="clientId ? String(clientId) : undefined"
          :options="clientOptions"
          all-label="Tüm Markalar"
          label="Marka"
          size="sm"
          class="max-w-48 flex-auto sm:flex-none"
          @update:model-value="(v) => (clientId = v ? Number(v) : undefined)"
        />
        <FilterSelect
          :model-value="type"
          :options="TYPE_OPTIONS"
          all-label="Tüm Türler"
          label="Tür"
          size="sm"
          class="flex-auto sm:flex-none"
          @update:model-value="(v) => (type = v as ContentType | undefined)"
        />
        <FilterSelect
          :model-value="due === 'all' ? undefined : due"
          :options="DUE_OPTIONS"
          all-label="Teslim: Tümü"
          prefix="Teslim: "
          label="Teslim"
          size="sm"
          class="flex-auto sm:flex-none"
          @update:model-value="(v) => (due = (v as DueFilter | undefined) ?? 'all')"
        />
      </div>
    </div>

    <div v-if="rows.error.value && !rows.data.value" role="alert" class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between">
      <p class="flex items-start gap-2 text-sm text-muted-foreground">
        <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
        <span><span class="font-medium text-foreground">İçerikler yüklenemedi.</span> {{ rows.error.value }}</span>
      </p>
      <Button variant="outline" :disabled="rows.loading.value" @click="rows.reload()">Tekrar dene</Button>
    </div>

    <EmptyState
      v-else-if="isEmpty && hasFilters"
      :icon="SearchXIcon"
      title="Bu filtrelere uyan içerik yok"
      description="Başka bir durum, marka ya da tür deneyin veya filtreleri temizleyin."
    >
      <Button variant="outline" @click="clearFilters">Filtreleri temizle</Button>
    </EmptyState>

    <EmptyState
      v-else-if="isEmpty"
      :icon="FileTextIcon"
      title="Henüz içerik yok"
      description="Markalarınızdan biri için ilk paylaşımı ya da sayfayı planlayın."
    >
      <Button @click="openNew()">Yeni İçerik</Button>
    </EmptyState>

    <div v-else :class="cn('rounded-card border bg-card shadow-card transition-opacity', rows.loading.value && rows.data.value && 'opacity-60')" :aria-busy="rows.loading.value">
      <!-- Desktop: table -->
      <div class="hidden px-3 pt-0.5 md:block">
        <!-- Fixed layout: the columns share the width the panel leaves; long titles wrap to two lines instead of pushing the status out of view. -->
        <table class="w-full table-fixed border-collapse text-sm">
          <thead>
            <tr class="text-left text-[13px] font-bold text-secondary-text">
              <th scope="col" class="w-[44%] px-2.5 py-3">İçerik</th>
              <!-- The type icon sits before each title, so there is no separate Tür column. -->
              <th scope="col" class="w-[24%] px-2.5 py-3">Marka</th>
              <th scope="col" class="w-[16%] px-2.5 py-3">Teslim</th>
              <th scope="col" class="w-[16%] px-2.5 py-3">
                <!--
                  Centred over the badges below: a hidden, zero-height uniform badge gives this cell
                  exactly their width, and the label sits on top of it. Screen readers only hear "Durum".
                  A block-level grid (not inline-grid), so the hidden badge cannot shift the text baseline.
                -->
                <span class="grid w-fit">
                  <StatusBadge status="PUBLISHED" uniform class="invisible col-start-1 row-start-1 h-0 overflow-hidden" aria-hidden="true" />
                  <span class="col-start-1 row-start-1 text-center">Durum</span>
                </span>
              </th>
            </tr>
          </thead>
          <tbody v-if="showSkeleton">
            <tr v-for="n in ROWS" :key="n" class="border-t border-divider" aria-hidden="true">
              <td class="p-2.5">
                <div class="flex items-center gap-3">
                  <Skeleton class="size-4 shrink-0 rounded-sm" />
                  <Skeleton class="h-3.5 w-full max-w-40" />
                </div>
              </td>
              <td class="p-2.5"><Skeleton class="h-3.5 w-full max-w-24" /></td>
              <td class="p-2.5"><Skeleton class="h-3.5 w-14" /></td>
              <td class="p-2.5"><Skeleton class="h-6 w-16 rounded-full" /></td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="item in items"
              :key="item.id"
              v-highlight:content="item.id"
              class="cursor-pointer border-t border-divider transition-colors hover:bg-coral-soft/40"
              @click="openEdit(item)"
            >
              <td class="p-2.5">
                <div class="flex items-start gap-3">
                  <!-- Icon only: ContentTypeIcon gives it a tooltip and an sr-only label with the full name. -->
                  <ContentTypeIcon :type="item.type" class="mt-0.5" />
                  <!-- A real button so keyboard users can open the sheet; the whole row is clickable too. -->
                  <button
                    type="button"
                    class="line-clamp-2 min-w-0 rounded-sm text-left leading-snug font-semibold break-words outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    :title="item.title"
                    @click.stop="openEdit(item)"
                  >
                    {{ item.title }}
                  </button>
                </div>
              </td>
              <td class="p-2.5 leading-snug break-words">{{ item.client.name }}</td>
              <td class="p-2.5 whitespace-nowrap">
                <span :class="item.overdue ? 'font-semibold text-overdue-text' : ''">{{ formatShortDate(item.dueDate) }}</span>
                <span v-if="dueHint(item)" :class="cn('block text-xs', item.overdue ? 'text-overdue-text' : 'text-muted-foreground')">
                  {{ dueHint(item) }}
                </span>
              </td>
              <td class="p-2.5" @click.stop>
                <StatusMenu :item="item" :pending="rowPending(item.id)" @move="(s) => move(item, s)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile: one compact row per item -->
      <ul class="divide-y divide-divider md:hidden">
        <template v-if="showSkeleton">
          <li v-for="n in 4" :key="n" class="flex items-start gap-3 p-3" aria-hidden="true">
            <Skeleton class="size-4 shrink-0 rounded-sm" />
            <div class="flex-1 space-y-2">
              <Skeleton class="h-3.5 w-3/4" />
              <Skeleton class="h-3 w-1/3" />
            </div>
          </li>
        </template>
        <li v-for="item in items" v-else :key="item.id" v-highlight:content="item.id" class="flex items-center gap-3 p-3">
          <!-- Same as the table: the type icon leads the row, where the thumbnail used to be. -->
          <ContentTypeIcon :type="item.type" class="mt-0.5 self-start" />
          <div class="min-w-0 flex-1">
            <button
              type="button"
              class="line-clamp-2 max-w-full rounded-sm text-left text-sm font-semibold outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              @click="openEdit(item)"
            >
              {{ item.title }}
            </button>
            <p class="mt-0.5 line-clamp-2 text-xs text-muted-foreground">
              {{ item.client.name }}&nbsp;·
              <span :class="cn('whitespace-nowrap', item.overdue && 'font-semibold text-overdue-text')">{{ dueHint(item) ?? formatShortDate(item.dueDate) }}</span>
            </p>
          </div>
          <StatusMenu :item="item" :pending="rowPending(item.id)" @move="(s) => move(item, s)" />
        </li>
      </ul>

      <RouterLink
        :to="allLink"
        class="block rounded-b-card px-5 py-2.5 text-[13px] font-semibold text-coral-text outline-none hover:underline hover:underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        Tüm içerikler →
      </RouterLink>
    </div>
  </section>
</template>
