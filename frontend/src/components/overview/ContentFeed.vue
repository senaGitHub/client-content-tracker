<script setup lang="ts">
import { CircleAlertIcon, FileTextIcon, SearchXIcon } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { contentsApi } from '@/api'
import type { ClientRef, ContentFilters, ContentResponse, ContentStatus, ContentType, StatusCounts } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import StatusBadge from '@/components/content/StatusBadge.vue'
import StatusMenu from '@/components/content/StatusMenu.vue'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { asPublishing, useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { STATUS_ORDER, STATUS_LABELS, TYPE_LABELS } from '@/lib/content'
import { dueHint, formatShortDate } from '@/lib/format'
import { cn } from '@/lib/utils'

const props = defineProps<{
  /** Brands for the filter (from the dashboard response, so no extra request). */
  clients: ClientRef[]
  /**
   * Approvals from the page's panel that are waiting to be sent: shown as published already.
   * Computed at display time, so "Geri al" or a failure puts them back at once.
   */
  publishing: ContentResponse[]
}>()
const emit = defineEmits<{
  /** A status change from this feed was confirmed (or rolled back) by the server. */
  changed: []
}>()

const ROWS = 6
const ALL = 'all'
const TYPES = Object.keys(TYPE_LABELS) as ContentType[]

const status = ref<ContentStatus>()
const clientId = ref<number>()
const type = ref<ContentType>()

const filters = computed<ContentFilters>(() => ({
  clientId: clientId.value,
  type: type.value,
}))
const hasFilters = computed(() => !!(clientId.value || type.value || status.value))

const { openNew, openEdit } = useContentDialog()

// Rows: what needs attention first (overdue, then nearest deadlines; published last). See ?sort=priority.
const SORT = 'priority'
const rows = useAsyncData(
  (signal) => contentsApi.list({ ...filters.value, status: status.value }, { size: ROWS, sort: SORT }, signal),
  { immediate: false },
)
// Tab counts follow the brand / type filters, never the selected status.
const counts = useAsyncData((signal) => contentsApi.statusCounts(filters.value, signal), { immediate: false })

watch(() => JSON.stringify([filters.value, status.value]), () => void rows.reload(), { immediate: true })
watch(() => JSON.stringify(filters.value), () => void counts.reload(), { immediate: true })
/**
 * Reloads rows and tab counts; the page awaits it after something outside the feed changed the
 * data (approvals, dialog saves), so an approval only stops counting as pending once fresh rows are in.
 */
async function refresh() {
  await Promise.all([rows.reload(), counts.reload()])
}
defineExpose({ refresh })

/** Waiting approvals that match the feed's brand / type filters (the tab counts follow those). */
const publishingInView = computed(() =>
  props.publishing.filter(
    (item) => (!clientId.value || item.client.id === clientId.value) && (!type.value || item.type === type.value),
  ),
)

const tabs = computed(() => {
  const server = counts.data.value
  // Every publish comes from Onayda, so a waiting one moves one count from Onayda to Yayında.
  const moved = publishingInView.value.length
  const c: StatusCounts | undefined = server && {
    ...server,
    inReview: server.inReview - moved,
    published: server.published + moved,
  }
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
function place(item: ContentResponse, phase: PlacePhase) {
  const page = rows.data.value
  if (page) rows.data.value = { ...page, items: page.items.map((row) => (row.id === item.id ? item : row)) }
  if (phase !== 'optimistic') {
    void counts.reload()
    emit('changed')
  }
}
const { move, isPending } = useStatusChange(place)

// ---- Display helpers ------------------------------------------------------------------------

const allLink = computed<RouteLocationRaw>(() => {
  const query: Record<string, string> = { view: 'list', sort: SORT }
  if (status.value) query.status = status.value
  if (clientId.value) query.clientId = String(clientId.value)
  if (type.value) query.type = type.value
  return { path: '/content', query }
})

function clearFilters() {
  status.value = undefined
  clientId.value = undefined
  type.value = undefined
}

// Rows keep their place; a waiting approval just shows as published (as in the content list).
const items = computed(() => {
  const list = rows.data.value?.items ?? []
  const waiting = new Set(props.publishing.map((item) => item.id))
  return waiting.size ? list.map((row) => (waiting.has(row.id) ? asPublishing(row) : row)) : list
})
const showSkeleton = computed(() => rows.loading.value && !rows.data.value)
const isEmpty = computed(() => !!rows.data.value && rows.data.value.totalElements === 0)
</script>

<template>
  <section aria-labelledby="overview-feed">
    <div class="mb-2.5 flex flex-wrap items-center gap-3">
      <h2 id="overview-feed" class="mr-1.5 text-section-title">İçerik akışı</h2>

      <div role="group" aria-label="Durum" class="no-scrollbar -mx-1 flex max-w-full gap-0.5 overflow-x-auto px-1">
        <button
          v-for="tab in tabs"
          :key="tab.label"
          type="button"
          :aria-pressed="status === tab.value"
          :class="cn(
            'shrink-0 rounded-lg px-3.5 py-2 text-sm whitespace-nowrap outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
            status === tab.value ? 'bg-coral-soft font-semibold text-coral-text' : 'font-medium text-secondary-text hover:bg-muted',
          )"
          @click="status = tab.value"
        >
          {{ tab.label }}<span v-if="tab.count !== undefined" class="tabular-nums"> ({{ tab.count }})</span>
        </button>
      </div>

      <div class="flex w-full gap-2 sm:ml-auto sm:w-auto">
        <Select
          :model-value="clientId ? String(clientId) : ALL"
          @update:model-value="(v) => (clientId = v === ALL ? undefined : Number(v))"
        >
          <SelectTrigger class="h-9 flex-1 bg-card font-medium sm:w-40 sm:flex-none" aria-label="Marka">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem :value="ALL">Tüm Markalar</SelectItem>
            <SelectItem v-for="client in clients" :key="client.id" :value="String(client.id)">{{ client.name }}</SelectItem>
          </SelectContent>
        </Select>
        <Select
          :model-value="type ?? ALL"
          @update:model-value="(v) => (type = v === ALL ? undefined : (v as ContentType))"
        >
          <SelectTrigger class="h-9 flex-1 bg-card font-medium sm:w-36 sm:flex-none" aria-label="Tür">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem :value="ALL">Tüm Türler</SelectItem>
            <SelectItem v-for="t in TYPES" :key="t" :value="t">{{ TYPE_LABELS[t] }}</SelectItem>
          </SelectContent>
        </Select>
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
                <StatusMenu :item="item" :pending="isPending(item.id)" @move="(s) => move(item, s)" />
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
        <li v-for="item in items" v-else :key="item.id" class="flex items-center gap-3 p-3">
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
          <StatusMenu :item="item" :pending="isPending(item.id)" @move="(s) => move(item, s)" />
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
