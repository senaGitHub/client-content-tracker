<script setup lang="ts">
import { CircleAlertIcon, FileTextIcon, SearchXIcon } from '@lucide/vue'
import { computed, watch } from 'vue'
import { contentsApi } from '@/api'
import type { ContentFilters, ContentResponse, ContentStatus } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import ContentActionsMenu from '@/components/content/ContentActionsMenu.vue'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import StatusMenu from '@/components/content/StatusMenu.vue'
import { Button } from '@/components/ui/button'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import type { ContentQuery } from '@/composables/useContentQuery'
import { asPublishing, useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { dueHint, formatDate, formatDateTime, formatShortDate, formatUpdated } from '@/lib/format'
import { cn } from '@/lib/utils'
import SortableHead from './SortableHead.vue'

const PAGE_SIZE = 20
/**
 * Default order: what needs attention first (unpublished by due date, so overdue leads; then
 * published, newest first). It is not a column, so no header shows an active arrow for it.
 */
const DEFAULT_SORT = 'priority'

const props = defineProps<{
  filters: ContentFilters
  status?: ContentStatus
  /** One-based. */
  page: number
  sort?: string
  hasFilters: boolean
}>()
const emit = defineEmits<{
  update: [patch: Partial<ContentQuery>, options?: { keepPage?: boolean }]
  clear: []
  /** A status change was confirmed or rolled back by the server (status counts may differ). */
  changed: []
}>()

const { openNew, openEdit, onSaved } = useContentDialog()

// ---- Status changes (rows keep their place; only the badge changes) ------------------------

function place(item: ContentResponse, phase: PlacePhase) {
  const page = data.value
  if (page) data.value = { ...page, items: page.items.map((row) => (row.id === item.id ? item : row)) }
  if (phase !== 'optimistic') emit('changed')
}

// Declared before the first load, which re-applies publishes that are still waiting.
const { move, isPending, publishingItems } = useStatusChange(place)

// ---- Loading -------------------------------------------------------------------------------

const request = computed(() => ({
  filters: { ...props.filters, status: props.status },
  paging: { page: props.page - 1, size: PAGE_SIZE, sort: props.sort ?? DEFAULT_SORT },
}))

const { data, error, loading, reload } = useAsyncData(
  async (signal) => {
    const page = await contentsApi.list(request.value.filters, request.value.paging, signal)
    // Publishes still waiting are not on the server yet: keep showing them as published.
    const waiting = new Set(publishingItems.value.map((item) => item.id))
    return waiting.size ? { ...page, items: page.items.map((row) => (waiting.has(row.id) ? asPublishing(row) : row)) } : page
  },
  { immediate: false, errorToast: 'İçerik listesi yüklenemedi' },
)
watch(() => JSON.stringify(request.value), reload, { immediate: true })
onSaved(() => reload())

// A page number from an old link may no longer exist: go to the last real page.
watch(data, (page) => {
  if (page && page.items.length === 0 && page.totalPages > 0 && props.page > page.totalPages) {
    emit('update', { page: page.totalPages }, { keepPage: true })
  }
})

// ---- Sorting & paging ----------------------------------------------------------------------

const currentSort = computed(() => {
  const [property = DEFAULT_SORT, direction] = (props.sort ?? DEFAULT_SORT).split(',')
  return { property, direction: direction === 'desc' ? ('desc' as const) : ('asc' as const) }
})

/** From the default order a header click sorts ascending; clicking the active header flips it. */
function onSort(property: string) {
  const current = currentSort.value
  const direction = current.property === property && current.direction === 'asc' ? 'desc' : 'asc'
  emit('update', { sort: `${property},${direction}` })
}

const range = computed(() => {
  const page = data.value
  if (!page || page.totalElements === 0) return ''
  const from = page.page * page.size + 1
  const to = from + page.items.length - 1
  return `${from}–${to} / ${page.totalElements} içerik gösteriliyor`
})

const showSkeleton = computed(() => loading.value && !data.value)
const isEmpty = computed(() => !!data.value && data.value.totalElements === 0)

const pagerButton = 'h-10 min-w-10 rounded-lg border-input bg-card px-4 text-sm font-semibold text-foreground hover:bg-muted'
</script>

<template>
  <div
    v-if="error && !data"
    role="alert"
    class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"
  >
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
      <span><span class="font-medium text-foreground">Liste yüklenemedi.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="reload">Tekrar dene</Button>
  </div>

  <EmptyState
    v-else-if="isEmpty && hasFilters"
    :icon="SearchXIcon"
    title="Bu filtrelere uyan içerik yok"
    description="Başka bir durum, marka, tür ya da teslim tarihi deneyin veya filtreleri temizleyin."
  >
    <Button variant="outline" @click="emit('clear')">Filtreleri temizle</Button>
  </EmptyState>

  <EmptyState
    v-else-if="isEmpty"
    :icon="FileTextIcon"
    title="Henüz içerik yok"
    description="Markalarınızdan biri için ilk paylaşımı ya da sayfayı planlayın."
  >
    <Button @click="openNew()">Yeni İçerik</Button>
  </EmptyState>

  <div
    v-else
    :class="cn('rounded-card border bg-card px-3.5 pt-1 pb-2 shadow-card transition-opacity', loading && data && 'opacity-60')"
    :aria-busy="loading"
  >
    <!-- Desktop: table -->
    <div class="hidden overflow-x-auto md:block">
      <table class="w-full min-w-[900px] border-collapse text-sm">
        <thead>
          <tr class="text-left">
            <SortableHead label="İçerik" property="title" :sort="currentSort" @sort="onSort" />
            <SortableHead label="Marka" property="client.name" :sort="currentSort" @sort="onSort" />
            <th scope="col" class="px-2.5 py-3 text-[13px] font-bold text-secondary-text">Tür</th>
            <SortableHead label="Teslim" property="dueDate" :sort="currentSort" @sort="onSort" />
            <th scope="col" class="px-2.5 py-3 text-[13px] font-bold text-secondary-text">Durum</th>
            <th scope="col" class="px-2.5 py-3 text-[13px] font-bold text-secondary-text">Güncelleme</th>
            <th scope="col" class="w-12 px-2.5 py-3"><span class="sr-only">İşlemler</span></th>
          </tr>
        </thead>

        <tbody v-if="showSkeleton">
          <tr v-for="n in 8" :key="n" class="border-t border-divider" aria-hidden="true">
            <td class="p-2.5">
              <div class="flex items-center gap-3">
                <Skeleton class="h-[30px] w-11 rounded-sm" />
                <Skeleton class="h-3.5 w-48" />
              </div>
            </td>
            <td class="p-2.5"><Skeleton class="h-3.5 w-28" /></td>
            <td class="p-2.5"><Skeleton class="h-3.5 w-24" /></td>
            <td class="p-2.5"><Skeleton class="h-3.5 w-20" /></td>
            <td class="p-2.5"><Skeleton class="h-6 w-20 rounded-full" /></td>
            <td class="p-2.5"><Skeleton class="h-3.5 w-16" /></td>
            <td class="p-2.5" />
          </tr>
        </tbody>

        <tbody v-else>
          <tr
            v-for="item in data?.items"
            :key="item.id"
            class="cursor-pointer border-t border-divider transition-colors hover:bg-coral-soft/40"
            @click="openEdit(item)"
          >
            <td class="max-w-[360px] p-2.5">
              <div class="flex items-center gap-3">
                <img
                  v-if="item.client.coverImageUrl"
                  :src="item.client.coverImageUrl"
                  alt=""
                  loading="lazy"
                  class="h-[30px] w-11 shrink-0 rounded-sm object-cover"
                />
                <span v-else class="h-[30px] w-11 shrink-0 rounded-sm bg-muted" aria-hidden="true" />
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
            <td class="max-w-48 p-2.5 leading-snug break-words">{{ item.client.name }}</td>
            <td class="p-2.5">
              <ContentTypeIcon :type="item.type" with-label class="gap-2 text-sm text-foreground" />
            </td>
            <td class="p-2.5 whitespace-nowrap">
              <span :class="item.overdue ? 'font-semibold text-overdue-text' : ''">{{ formatDate(item.dueDate) }}</span>
              <span v-if="dueHint(item)" :class="cn('block text-xs', item.overdue ? 'text-overdue-text' : 'text-muted-foreground')">
                {{ dueHint(item) }}
              </span>
            </td>
            <td class="p-2.5" @click.stop>
              <StatusMenu :item="item" :pending="isPending(item.id)" @move="(status) => move(item, status)" />
            </td>
            <td class="p-2.5 text-[13px] whitespace-nowrap text-muted-foreground">
              <time :datetime="item.updatedAt" :title="formatDateTime(item.updatedAt)">{{ formatUpdated(item.updatedAt) }}</time>
            </td>
            <td class="p-2.5 text-right" @click.stop>
              <ContentActionsMenu
                :item="item"
                :pending="isPending(item.id)"
                class="ml-auto"
                @open="openEdit(item)"
                @move="(status) => move(item, status)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Mobile: one compact row per item -->
    <ul class="divide-y divide-divider md:hidden">
      <template v-if="showSkeleton">
        <li v-for="n in 5" :key="n" class="flex items-center gap-3 py-3" aria-hidden="true">
          <Skeleton class="h-10 w-14 rounded-md" />
          <div class="flex-1 space-y-2">
            <Skeleton class="h-3.5 w-3/4" />
            <Skeleton class="h-3 w-1/3" />
          </div>
        </li>
      </template>
      <li v-for="item in data?.items" v-else :key="item.id" class="flex items-center gap-3 py-3">
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
            class="block max-w-full truncate rounded-sm text-left text-sm font-semibold outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            @click="openEdit(item)"
          >
            {{ item.title }}
          </button>
          <p class="mt-0.5 flex items-center gap-1.5 truncate text-xs text-muted-foreground">
            <ContentTypeIcon :type="item.type" />
            {{ item.client.name }} ·
            <span :class="item.overdue ? 'font-semibold text-overdue-text' : ''">{{ dueHint(item) ?? formatShortDate(item.dueDate) }}</span>
          </p>
        </div>
        <StatusMenu :item="item" :pending="isPending(item.id)" @move="(status) => move(item, status)" />
      </li>
    </ul>

    <!-- Paging -->
    <div v-if="data && data.totalElements > 0" class="flex flex-col items-center gap-3 px-2.5 pt-3.5 pb-1.5 sm:flex-row sm:justify-between">
      <p class="text-[13px] text-muted-foreground tabular-nums" aria-live="polite">{{ range }}</p>
      <Pagination
        :page="page"
        :total="data.totalElements"
        :items-per-page="PAGE_SIZE"
        :sibling-count="1"
        show-edges
        class="mx-0 w-auto"
        @update:page="(p: number) => emit('update', { page: p }, { keepPage: true })"
      >
        <PaginationContent v-slot="{ items }" class="gap-1.5">
          <PaginationPrevious :class="cn(pagerButton, 'pl-4!')">Önceki</PaginationPrevious>
          <template v-for="(entry, index) in items" :key="index">
            <PaginationItem
              v-if="entry.type === 'page'"
              :value="entry.value"
              :is-active="entry.value === page"
              :class="cn(pagerButton, 'px-3', entry.value === page && 'border-nav-active bg-coral-soft text-coral-text hover:bg-coral-soft')"
            >
              {{ entry.value }}
            </PaginationItem>
            <PaginationEllipsis v-else :index="index" />
          </template>
          <PaginationNext :class="cn(pagerButton, 'pr-4!')">Sonraki</PaginationNext>
        </PaginationContent>
      </Pagination>
    </div>
  </div>
</template>
