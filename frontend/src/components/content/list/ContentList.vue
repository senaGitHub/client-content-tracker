<script setup lang="ts">
import { CircleAlertIcon, FileTextIcon, PencilIcon, SearchXIcon } from '@lucide/vue'
import { computed, watch } from 'vue'
import { contentsApi } from '@/api'
import type { ContentFilters, ContentResponse, ContentStatus } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
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
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import type { ContentQuery } from '@/composables/useContentQuery'
import { useStatusChange } from '@/composables/useStatusChange'
import { dueLabel, formatDateTime, formatRelative, formatShortDate } from '@/lib/format'
import { cn } from '@/lib/utils'
import SortableHead from './SortableHead.vue'

const PAGE_SIZE = 20
const DEFAULT_SORT = { property: 'dueDate', direction: 'asc' as const }

const props = defineProps<{
  filters: ContentFilters
  status?: ContentStatus
  /** One-based. */
  page: number
  sort?: string
  hasFilters: boolean
}>()
const emit = defineEmits<{ update: [patch: Partial<ContentQuery>, options?: { keepPage?: boolean }]; clear: [] }>()

const { openNew, openEdit, onSaved } = useContentDialog()

const request = computed(() => ({
  filters: { ...props.filters, status: props.status },
  paging: { page: props.page - 1, size: PAGE_SIZE, sort: props.sort },
}))

const { data, error, loading, reload } = useAsyncData(
  (signal) => contentsApi.list(request.value.filters, request.value.paging, signal),
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

// ---- Status changes (rows keep their place; only the badge changes) ------------------------

function place(item: ContentResponse) {
  const page = data.value
  if (!page) return
  data.value = { ...page, items: page.items.map((row) => (row.id === item.id ? item : row)) }
}

const { move, isPending } = useStatusChange(place)

// ---- Sorting & paging ----------------------------------------------------------------------

const currentSort = computed(() => {
  if (!props.sort) return DEFAULT_SORT
  const [property = DEFAULT_SORT.property, direction] = props.sort.split(',')
  return { property, direction: direction === 'desc' ? ('desc' as const) : ('asc' as const) }
})

function onSort(property: string) {
  const current = currentSort.value
  const direction = current.property === property && current.direction === 'asc' ? 'desc' : 'asc'
  const next = `${property},${direction}`
  // The default order is left out of the URL.
  emit('update', { sort: next === 'dueDate,asc' ? undefined : next })
}

const range = computed(() => {
  const page = data.value
  if (!page || page.totalElements === 0) return ''
  const from = page.page * page.size + 1
  const to = from + page.items.length - 1
  return `${from}–${to} of ${page.totalElements}`
})

const showSkeleton = computed(() => loading.value && !data.value)
const isEmpty = computed(() => !!data.value && data.value.totalElements === 0)
</script>

<template>
  <div v-if="error && !data" role="alert" class="flex flex-col items-start gap-3 rounded-xl border p-5 sm:flex-row sm:items-center sm:justify-between">
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
      <span><span class="font-medium text-foreground">The list could not be loaded.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="reload">Try again</Button>
  </div>

  <EmptyState
    v-else-if="isEmpty && hasFilters"
    :icon="SearchXIcon"
    title="No content matches these filters"
    description="Try another client, type, status or due date, or clear the filters."
  >
    <Button variant="outline" @click="emit('clear')">Clear filters</Button>
  </EmptyState>

  <EmptyState
    v-else-if="isEmpty"
    :icon="FileTextIcon"
    title="No content yet"
    description="Plan the first post or page for one of your clients."
  >
    <Button @click="openNew()">New content</Button>
  </EmptyState>

  <div v-else class="flex flex-col gap-4" :aria-busy="loading">
    <!-- Desktop: table -->
    <div :class="cn('hidden overflow-hidden rounded-xl border transition-opacity md:block', loading && data && 'opacity-60')">
      <Table>
        <TableHeader>
          <TableRow class="hover:bg-transparent">
            <SortableHead label="Title" property="title" :sort="currentSort" @sort="onSort" />
            <SortableHead label="Client" property="client.name" :sort="currentSort" @sort="onSort" />
            <SortableHead label="Due" property="dueDate" :sort="currentSort" @sort="onSort" />
            <SortableHead label="Status" property="status" :sort="currentSort" @sort="onSort" />
            <SortableHead label="Updated" property="updatedAt" :sort="currentSort" @sort="onSort" />
            <TableHead class="w-12"><span class="sr-only">Actions</span></TableHead>
          </TableRow>
        </TableHeader>

        <TableBody v-if="showSkeleton">
          <TableRow v-for="n in 8" :key="n" class="hover:bg-transparent">
            <TableCell>
              <div class="flex items-center gap-3">
                <Skeleton class="size-8 rounded-lg" />
                <Skeleton class="h-3.5 w-48" />
              </div>
            </TableCell>
            <TableCell><Skeleton class="h-3.5 w-28" /></TableCell>
            <TableCell><Skeleton class="h-3.5 w-20" /></TableCell>
            <TableCell><Skeleton class="h-5 w-20 rounded-full" /></TableCell>
            <TableCell><Skeleton class="h-3.5 w-16" /></TableCell>
            <TableCell />
          </TableRow>
        </TableBody>

        <TableBody v-else>
          <TableRow v-for="item in data?.items" :key="item.id">
            <TableCell class="max-w-80">
              <div class="flex items-center gap-3">
                <ContentTypeIcon :type="item.type" />
                <div class="min-w-0">
                  <button
                    type="button"
                    class="block max-w-full truncate rounded-sm text-left text-sm font-medium text-foreground outline-none hover:underline hover:underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50"
                    @click="openEdit(item)"
                  >
                    {{ item.title }}
                  </button>
                  <span v-if="item.channel" class="block truncate text-xs text-muted-foreground">{{ item.channel }}</span>
                </div>
              </div>
            </TableCell>
            <TableCell class="max-w-48 truncate text-muted-foreground">{{ item.client.name }}</TableCell>
            <TableCell>
              <span :class="cn('block text-sm', item.overdue ? 'font-medium text-overdue-text' : 'text-foreground')">
                {{ dueLabel(item) }}
              </span>
              <span class="block text-xs text-muted-foreground">{{ formatShortDate(item.dueDate) }}</span>
            </TableCell>
            <TableCell>
              <StatusMenu :item="item" :pending="isPending(item.id)" @move="(status) => move(item, status)" />
            </TableCell>
            <TableCell class="text-muted-foreground">
              <time :datetime="item.updatedAt" :title="formatDateTime(item.updatedAt)">{{ formatRelative(item.updatedAt) }}</time>
            </TableCell>
            <TableCell class="text-right">
              <Button variant="ghost" size="icon-sm" :aria-label="`Edit ${item.title}`" @click="openEdit(item)">
                <PencilIcon />
              </Button>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>

    <!-- Mobile: one card per item -->
    <ul :class="cn('flex flex-col gap-2 md:hidden', loading && data && 'opacity-60')">
      <template v-if="showSkeleton">
        <li v-for="n in 5" :key="n" class="rounded-xl border p-3" aria-hidden="true">
          <Skeleton class="h-3.5 w-3/4" />
          <Skeleton class="mt-2 h-3 w-1/3" />
          <Skeleton class="mt-3 h-5 w-20 rounded-full" />
        </li>
      </template>
      <li v-for="item in data?.items" v-else :key="item.id" class="rounded-xl border p-3">
        <div class="flex items-start gap-3">
          <ContentTypeIcon :type="item.type" />
          <div class="min-w-0 flex-1">
            <button
              type="button"
              class="block max-w-full text-left text-sm leading-snug font-medium text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              @click="openEdit(item)"
            >
              {{ item.title }}
            </button>
            <p class="mt-0.5 truncate text-xs text-muted-foreground">
              {{ item.client.name }}<template v-if="item.channel"> · {{ item.channel }}</template>
            </p>
          </div>
        </div>
        <div class="mt-3 flex items-center justify-between gap-2">
          <StatusMenu :item="item" :pending="isPending(item.id)" @move="(status) => move(item, status)" />
          <span :class="cn('text-xs', item.overdue ? 'font-medium text-overdue-text' : 'text-muted-foreground')">
            {{ dueLabel(item) }}
          </span>
        </div>
      </li>
    </ul>

    <!-- Paging -->
    <div v-if="data && data.totalElements > 0" class="flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
      <p class="text-sm text-muted-foreground tabular-nums" aria-live="polite">{{ range }}</p>
      <Pagination
        v-if="data.totalPages > 1"
        :page="page"
        :total="data.totalElements"
        :items-per-page="PAGE_SIZE"
        :sibling-count="1"
        show-edges
        class="mx-0 w-auto"
        @update:page="(p: number) => emit('update', { page: p }, { keepPage: true })"
      >
        <PaginationContent v-slot="{ items }">
          <PaginationPrevious />
          <template v-for="(entry, index) in items" :key="index">
            <PaginationItem v-if="entry.type === 'page'" :value="entry.value" :is-active="entry.value === page">
              {{ entry.value }}
            </PaginationItem>
            <PaginationEllipsis v-else :index="index" />
          </template>
          <PaginationNext />
        </PaginationContent>
      </Pagination>
    </div>
  </div>
</template>
