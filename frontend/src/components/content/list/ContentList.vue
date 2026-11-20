<script setup lang="ts">
import { CircleAlertIcon, FileTextIcon, SearchXIcon } from '@lucide/vue'
import { computed, watch } from 'vue'
import { contentsApi } from '@/api'
import type { ContentFilters, ContentResponse, ContentStatus } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import ListPager from '@/components/common/ListPager.vue'
import ContentActionsMenu from '@/components/content/ContentActionsMenu.vue'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import StatusBadge from '@/components/content/StatusBadge.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import type { ContentQuery } from '@/composables/useContentQuery'
import { asPublishing, useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { vHighlight } from '@/composables/useHighlight'
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
  /**
   * Inside one brand's page: no Marka or Güncelleme columns and no column sorting (always
   * priority order), as in the brand detail mockup.
   */
  brand?: boolean
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
  paging: { page: props.page - 1, size: PAGE_SIZE, sort: (!props.brand && props.sort) || DEFAULT_SORT },
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

const showSkeleton = computed(() => loading.value && !data.value)
const isEmpty = computed(() => !!data.value && data.value.totalElements === 0)
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
    :title="brand ? 'Bu durumda içerik yok' : 'Bu filtrelere uyan içerik yok'"
    :description="brand ? 'Başka bir durum sekmesi seçin.' : 'Başka bir durum, marka, tür ya da teslim tarihi deneyin veya filtreleri temizleyin.'"
  >
    <Button variant="outline" @click="emit('clear')">{{ brand ? 'Tümünü göster' : 'Filtreleri temizle' }}</Button>
  </EmptyState>

  <EmptyState
    v-else-if="isEmpty"
    :icon="FileTextIcon"
    title="Henüz içerik yok"
    :description="brand ? 'Bu marka için ilk paylaşımı ya da sayfayı planlayın.' : 'Markalarınızdan biri için ilk paylaşımı ya da sayfayı planlayın.'"
  >
    <Button @click="openNew({ clientId: filters.clientId })">Yeni İçerik</Button>
  </EmptyState>

  <div
    v-else
    :class="cn('rounded-card border bg-card px-3.5 pt-1 pb-2 shadow-card transition-opacity', loading && data && 'opacity-60')"
    :aria-busy="loading"
  >
    <!-- Desktop: table -->
    <div class="hidden overflow-x-auto md:block">
      <table :class="cn('w-full border-collapse text-sm', brand ? 'min-w-[680px]' : 'min-w-[900px]')">
        <thead v-if="brand">
          <tr class="text-left text-[13px] font-bold text-secondary-text">
            <th scope="col" class="px-2.5 py-3">İçerik</th>
            <th scope="col" class="px-2.5 py-3">Tür</th>
            <th scope="col" class="px-2.5 py-3">Teslim</th>
            <th scope="col" class="px-2.5 py-3">Durum</th>
            <th scope="col" class="w-12 px-2.5 py-3"><span class="sr-only">İşlemler</span></th>
          </tr>
        </thead>
        <thead v-else>
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
            <td class="p-2.5"><Skeleton class="h-3.5 w-48" /></td>
            <td v-if="!brand" class="p-2.5"><Skeleton class="h-3.5 w-28" /></td>
            <td class="p-2.5"><Skeleton class="h-3.5 w-24" /></td>
            <td class="p-2.5"><Skeleton class="h-3.5 w-20" /></td>
            <td class="p-2.5"><Skeleton class="h-6 w-20 rounded-full" /></td>
            <td v-if="!brand" class="p-2.5"><Skeleton class="h-3.5 w-16" /></td>
            <td class="p-2.5" />
          </tr>
        </tbody>

        <tbody v-else>
          <tr
            v-for="item in data?.items"
            :key="item.id"
            v-highlight:content="item.id"
            class="cursor-pointer border-t border-divider transition-colors hover:bg-coral-soft/40"
            @click="openEdit(item)"
          >
            <td class="max-w-[360px] p-2.5">
              <!-- A real button so keyboard users can open the sheet; the whole row is clickable too. -->
              <button
                type="button"
                class="line-clamp-2 min-w-0 rounded-sm text-left leading-snug font-semibold break-words outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                :title="item.title"
                @click.stop="openEdit(item)"
              >
                {{ item.title }}
              </button>
              <p v-if="item.channel" class="mt-0.5 truncate text-xs text-muted-foreground">{{ item.channel }}</p>
            </td>
            <td v-if="!brand" class="max-w-48 p-2.5 leading-snug break-words">{{ item.client.name }}</td>
            <td class="p-2.5">
              <ContentTypeIcon :type="item.type" with-label class="gap-2 text-sm text-foreground" />
            </td>
            <td class="p-2.5 whitespace-nowrap">
              <span :class="item.overdue ? 'font-semibold text-overdue-text' : ''">{{ formatDate(item.dueDate) }}</span>
              <span v-if="dueHint(item)" :class="cn('block text-xs', item.overdue ? 'text-overdue-text' : 'text-muted-foreground')">
                {{ dueHint(item) }}
              </span>
            </td>
            <!-- Read-only here: status changes go through the ⋯ menu (which also shows the pending spinner). -->
            <td class="p-2.5">
              <StatusBadge :status="item.status" uniform />
            </td>
            <td v-if="!brand" class="p-2.5 text-[13px] whitespace-nowrap text-muted-foreground">
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
        <li v-for="n in 5" :key="n" class="flex items-center gap-2 py-3" aria-hidden="true">
          <div class="flex-1 space-y-2">
            <Skeleton class="h-3.5 w-3/4" />
            <Skeleton class="h-3 w-1/3" />
          </div>
          <Skeleton class="h-6 w-20 rounded-full" />
          <Skeleton class="size-8 rounded-lg" />
        </li>
      </template>
      <li v-for="item in data?.items" v-else :key="item.id" v-highlight:content="item.id" class="flex items-center gap-2 py-3">
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
            <template v-if="item.channel">{{ item.channel }}&nbsp;· </template>
            <template v-if="!brand">{{ item.client.name }}&nbsp;· </template>
            <span :class="cn('whitespace-nowrap', item.overdue && 'font-semibold text-overdue-text')">{{ dueHint(item) ?? formatShortDate(item.dueDate) }}</span>
          </p>
        </div>
        <!-- Same as the table: a read-only badge, and status changes through the ⋯ menu. -->
        <StatusBadge :status="item.status" uniform />
        <ContentActionsMenu
          :item="item"
          :pending="isPending(item.id)"
          class="-mr-1.5"
          @open="openEdit(item)"
          @move="(status) => move(item, status)"
        />
      </li>
    </ul>

    <!-- Paging -->
    <ListPager
      v-if="data"
      :page="page"
      :page-size="PAGE_SIZE"
      :total="data.totalElements"
      :shown="data.items.length"
      @update:page="(p) => emit('update', { page: p }, { keepPage: true })"
    />
  </div>
</template>
