<script setup lang="ts">
import { CircleAlertIcon, EllipsisIcon, FileTextIcon, SearchXIcon } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { contentsApi } from '@/api'
import type { ClientRef, ContentFilters, ContentResponse, ContentStatus, ContentType, StatusCounts } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import StatusMenu from '@/components/content/StatusMenu.vue'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { STATUS_ORDER, STATUS_LABELS, TYPE_LABELS } from '@/lib/content'
import { dueHint, formatShortDate } from '@/lib/format'
import { cn } from '@/lib/utils'

const props = defineProps<{
  /** Brands for the filter (from the dashboard response, so no extra request). */
  clients: ClientRef[]
  /** Search text from the page header (already debounced). */
  q: string
  /** Bumped by the page when something else changed the data (approvals, dialog saves). */
  version: number
}>()
const emit = defineEmits<{
  /** A status change from this feed was confirmed (or rolled back) by the server. */
  changed: []
  clearSearch: []
}>()

const ROWS = 6
const ALL = 'all'
const TYPES = Object.keys(TYPE_LABELS) as ContentType[]

const status = ref<ContentStatus>()
const clientId = ref<number>()
const type = ref<ContentType>()

const filters = computed<ContentFilters>(() => ({
  q: props.q.trim() || undefined,
  clientId: clientId.value,
  type: type.value,
}))
const hasFilters = computed(() => !!(filters.value.q || clientId.value || type.value || status.value))

const { openNew, openEdit } = useContentDialog()

// Rows: what needs attention first (overdue, then nearest deadlines; published last). See ?sort=priority.
const SORT = 'priority'
const rows = useAsyncData(
  (signal) => contentsApi.list({ ...filters.value, status: status.value }, { size: ROWS, sort: SORT }, signal),
  { immediate: false },
)
// Tab counts follow the brand / type / search filters, never the selected status.
const counts = useAsyncData((signal) => contentsApi.statusCounts(filters.value, signal), { immediate: false })

watch(() => JSON.stringify([filters.value, status.value]), () => void rows.reload(), { immediate: true })
watch(() => JSON.stringify(filters.value), () => void counts.reload(), { immediate: true })
watch(() => props.version, () => {
  void rows.reload()
  void counts.reload()
})

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
  if (filters.value.q) query.q = filters.value.q
  return { path: '/content', query }
})

function clearFilters() {
  status.value = undefined
  clientId.value = undefined
  type.value = undefined
  emit('clearSearch')
}

const items = computed(() => rows.data.value?.items ?? [])
const showSkeleton = computed(() => rows.loading.value && !rows.data.value)
const isEmpty = computed(() => !!rows.data.value && rows.data.value.totalElements === 0)
</script>

<template>
  <section aria-labelledby="overview-feed">
    <div class="mb-2.5 flex flex-wrap items-center gap-3">
      <h2 id="overview-feed" class="mr-1.5 text-lg font-bold">İçerik akışı</h2>

      <div role="group" aria-label="Durum" class="-mx-1 flex max-w-full gap-0.5 overflow-x-auto px-1">
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
      <div class="@container hidden px-3 pt-0.5 md:block">
        <!-- Fixed layout: the columns share the width the panel leaves; long titles wrap to two lines instead of pushing the actions out of view. -->
        <table class="w-full table-fixed border-collapse text-sm">
          <thead>
            <tr class="text-left text-[13px] font-bold text-secondary-text">
              <th scope="col" class="px-2.5 py-3">İçerik</th>
              <th scope="col" class="w-[152px] px-2.5 py-3">Marka</th>
              <th scope="col" class="w-12 px-2.5 py-3 @4xl:w-36">Tür</th>
              <th scope="col" class="w-[92px] px-2.5 py-3">Teslim</th>
              <th scope="col" class="w-28 px-2.5 py-3">Durum</th>
              <th scope="col" class="w-[100px] px-1.5 py-3"><span class="sr-only">İşlemler</span></th>
            </tr>
          </thead>
          <tbody v-if="showSkeleton">
            <tr v-for="n in ROWS" :key="n" class="border-t border-divider" aria-hidden="true">
              <td class="p-2.5">
                <div class="flex items-center gap-3">
                  <Skeleton class="h-[30px] w-11 rounded-sm" />
                  <Skeleton class="h-3.5 w-full max-w-40" />
                </div>
              </td>
              <td class="p-2.5"><Skeleton class="h-3.5 w-full max-w-24" /></td>
              <td class="p-2.5"><Skeleton class="h-3.5 w-full max-w-24" /></td>
              <td class="p-2.5"><Skeleton class="h-3.5 w-14" /></td>
              <td class="p-2.5"><Skeleton class="h-6 w-16 rounded-full" /></td>
              <td class="p-2.5" />
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="item in items"
              :key="item.id"
              class="group cursor-pointer border-t border-divider transition-colors hover:bg-coral-soft/40"
              @click="openEdit(item)"
            >
              <td class="p-2.5">
                <div class="flex items-center gap-3">
                  <img
                    v-if="item.client.coverImageUrl"
                    :src="item.client.coverImageUrl"
                    alt=""
                    loading="lazy"
                    class="h-[30px] w-11 shrink-0 rounded-sm object-cover"
                  />
                  <span v-else class="h-[30px] w-11 shrink-0 rounded-sm bg-muted" aria-hidden="true" />
                  <span class="line-clamp-2 min-w-0 leading-snug font-semibold break-words" :title="item.title">{{ item.title }}</span>
                </div>
              </td>
              <td class="p-2.5 leading-snug break-words">{{ item.client.name }}</td>
              <td class="p-2.5">
                <!-- The label needs room; in a narrow panel the icon alone (with a tooltip) stands in for it. -->
                <ContentTypeIcon :type="item.type" with-label class="hidden max-w-full gap-2 text-sm text-foreground @4xl:inline-flex" />
                <ContentTypeIcon :type="item.type" class="@4xl:hidden" />
              </td>
              <td class="p-2.5 whitespace-nowrap">
                <span :class="item.overdue ? 'font-semibold text-overdue-text' : ''">{{ formatShortDate(item.dueDate) }}</span>
                <span v-if="dueHint(item)" :class="cn('block text-xs', item.overdue ? 'text-overdue-text' : 'text-muted-foreground')">
                  {{ dueHint(item) }}
                </span>
              </td>
              <td class="p-2.5" @click.stop>
                <StatusMenu :item="item" :pending="isPending(item.id)" @move="(s) => move(item, s)" />
              </td>
              <td class="px-1.5 py-2.5 text-right">
                <button
                  type="button"
                  :aria-label="`Düzenle: ${item.title}`"
                  class="inline-flex h-7 items-center justify-end rounded-md px-2 text-[13px] font-semibold text-secondary-text outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50"
                  @click.stop="openEdit(item)"
                >
                  <EllipsisIcon class="size-5 group-hover:hidden group-focus-within:hidden" aria-hidden="true" />
                  <span class="hidden text-coral-text group-hover:inline group-focus-within:inline" aria-hidden="true">Düzenle →</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile: one compact row per item -->
      <ul class="divide-y divide-divider md:hidden">
        <template v-if="showSkeleton">
          <li v-for="n in 4" :key="n" class="flex items-center gap-3 p-3" aria-hidden="true">
            <Skeleton class="h-10 w-14 rounded-md" />
            <div class="flex-1 space-y-2">
              <Skeleton class="h-3.5 w-3/4" />
              <Skeleton class="h-3 w-1/3" />
            </div>
          </li>
        </template>
        <li v-for="item in items" v-else :key="item.id" class="flex items-center gap-3 p-3">
          <img
            v-if="item.client.coverImageUrl"
            :src="item.client.coverImageUrl"
            alt=""
            loading="lazy"
            class="h-10 w-14 shrink-0 rounded-md object-cover"
          />
          <div class="min-w-0 flex-1">
            <button
              type="button"
              class="line-clamp-2 max-w-full rounded-sm text-left text-sm font-semibold outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              @click="openEdit(item)"
            >
              {{ item.title }}
            </button>
            <p class="mt-0.5 line-clamp-2 text-xs text-muted-foreground">
              <ContentTypeIcon :type="item.type" class="mr-1 align-[-4px]" />
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
