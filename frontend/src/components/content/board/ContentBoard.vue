<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { CircleAlertIcon, FileTextIcon, SearchXIcon } from '@lucide/vue'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import type { DraggableEvent } from 'vue-draggable-plus'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import type { ContentFilters, ContentResponse, ContentStatus } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import { errorMessage } from '@/lib/errors'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useContentDialog } from '@/composables/useContentDialog'
import { useStatusChange } from '@/composables/useStatusChange'
import { STATUS_LABELS, STATUS_ORDER, STATUS_STYLES } from '@/lib/content'
import { cn } from '@/lib/utils'
import BoardCard from './BoardCard.vue'

const PAGE_SIZE = 20

const props = defineProps<{ filters: ContentFilters; hasFilters: boolean }>()
const emit = defineEmits<{ clear: [] }>()

interface Column {
  items: ContentResponse[]
  total: number
  page: number
  hasNext: boolean
  loadingMore: boolean
}

function emptyColumn(): Column {
  return { items: [], total: 0, page: 0, hasNext: false, loadingMore: false }
}

const columns = reactive(
  Object.fromEntries(STATUS_ORDER.map((s) => [s, emptyColumn()])) as Record<ContentStatus, Column>,
)
const loading = ref(true)
const error = ref<string | null>(null)
// Which column each loaded card is in, so totals stay right however the card got moved.
const statusById = new Map<number, ContentStatus>()

const { openNew, openEdit, onSaved } = useContentDialog()

// ---- Loading -------------------------------------------------------------------------------

let controller: AbortController | null = null

function fetchColumn(status: ContentStatus, page: number, signal?: AbortSignal) {
  return contentsApi.list({ ...props.filters, status }, { page, size: PAGE_SIZE }, signal)
}

async function loadAll() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = null
  try {
    // One request per column, in parallel: each column pages on its own.
    const pages = await Promise.all(STATUS_ORDER.map((s) => fetchColumn(s, 0, current.signal)))
    statusById.clear()
    STATUS_ORDER.forEach((status, i) => {
      const page = pages[i]!
      Object.assign(columns[status], {
        items: page.items,
        total: page.totalElements,
        page: 0,
        hasNext: page.hasNext,
        loadingMore: false,
      })
      page.items.forEach((item) => statusById.set(item.id, status))
    })
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = errorMessage(e)
    toast.error('Pano yüklenemedi', { description: error.value })
  } finally {
    if (controller === current) loading.value = false
  }
}

async function loadMore(status: ContentStatus) {
  const column = columns[status]
  if (column.loadingMore || !column.hasNext) return
  column.loadingMore = true
  try {
    const page = await fetchColumn(status, column.page + 1)
    // Cards moved since the first load shift page offsets, so skip anything already shown.
    const fresh = page.items.filter((item) => !statusById.has(item.id))
    fresh.forEach((item) => statusById.set(item.id, status))
    column.items.push(...fresh)
    column.page = page.page
    column.hasNext = page.hasNext
    column.total = page.totalElements
  } catch (e) {
    toast.error('Daha fazla içerik yüklenemedi', { description: errorMessage(e) })
  } finally {
    column.loadingMore = false
  }
}

watch(() => JSON.stringify(props.filters), loadAll, { immediate: true })
onSaved(() => loadAll())
onBeforeUnmount(() => controller?.abort())

// ---- Moving cards --------------------------------------------------------------------------

function byDueDate(a: ContentResponse, b: ContentResponse) {
  return a.dueDate.localeCompare(b.dueDate) || a.id - b.id
}

/** Shows `item` in the column of its status, sorted by due date, and keeps totals in step. */
function place(item: ContentResponse) {
  const previous = statusById.get(item.id)
  if (previous && previous !== item.status) {
    columns[previous].total--
    columns[item.status].total++
  }
  statusById.set(item.id, item.status)
  for (const status of STATUS_ORDER) {
    const list = columns[status].items
    const index = list.findIndex((i) => i.id === item.id)
    if (index !== -1) list.splice(index, 1)
  }
  const target = columns[item.status].items
  const at = target.findIndex((other) => byDueDate(item, other) < 0)
  target.splice(at === -1 ? target.length : at, 0, item)
}

const { move, isPending } = useStatusChange(place)

// While a card is dragged, columns it may go to are highlighted and the others fade.
const dragging = ref<ContentResponse | null>(null)

function columnHint(status: ContentStatus) {
  const item = dragging.value
  if (!item || item.status === status) return ''
  return item.allowedTransitions.includes(status)
    ? 'border-primary/50 bg-primary/5'
    : 'opacity-50'
}

function onDragStart(event: DraggableEvent<ContentResponse>) {
  dragging.value = event.data
}

function onDragEnd() {
  dragging.value = null
}

/** A card was dropped here. The server decides; a refused move snaps back with an explanation. */
function onDrop(status: ContentStatus, event: DraggableEvent<ContentResponse>) {
  const item = event.data
  if (item && item.status !== status) move(item, status)
}

// ---- Layout --------------------------------------------------------------------------------

// Three columns don't fit on a phone: show one at a time and move cards with the card menu.
const isDesktop = useMediaQuery('(min-width: 768px)')
const mobileColumn = ref<ContentStatus>('DRAFT')
const visibleStatuses = computed(() => (isDesktop.value ? STATUS_ORDER : [mobileColumn.value]))

const isEmpty = computed(() => !loading.value && !error.value && STATUS_ORDER.every((s) => columns[s].total === 0))

const EMPTY_COLUMN_TEXT: Record<ContentStatus, string> = {
  DRAFT: 'No drafts',
  IN_REVIEW: 'Nothing in review',
  PUBLISHED: 'Nothing published yet',
}
</script>

<template>
  <div v-if="error" role="alert" class="flex flex-col items-start gap-3 rounded-xl border p-5 sm:flex-row sm:items-center sm:justify-between">
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
      <span><span class="font-medium text-foreground">The board could not be loaded.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="loadAll">Try again</Button>
  </div>

  <EmptyState
    v-else-if="isEmpty && hasFilters"
    :icon="SearchXIcon"
    title="No content matches these filters"
    description="Try another client, type or due date, or clear the filters."
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

  <div v-else class="flex flex-col gap-3">
    <!-- Mobile column switcher -->
    <div v-if="!isDesktop" role="tablist" aria-label="Board column" class="grid grid-cols-3 gap-1 rounded-lg bg-muted p-0.5">
      <button
        v-for="status in STATUS_ORDER"
        :key="status"
        type="button"
        role="tab"
        :aria-selected="mobileColumn === status"
        :class="cn(
          'flex h-9 items-center justify-center gap-1.5 rounded-md text-sm font-medium outline-none focus-visible:ring-3 focus-visible:ring-ring/50',
          mobileColumn === status ? 'border bg-card text-foreground' : 'text-muted-foreground',
        )"
        @click="mobileColumn = status"
      >
        <span :class="cn('size-2 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
        <span class="truncate">{{ STATUS_LABELS[status] }}</span>
        <span v-if="!loading" class="text-xs text-muted-foreground tabular-nums">{{ columns[status].total }}</span>
      </button>
    </div>

    <div class="grid gap-3 md:grid-cols-3" :aria-busy="loading">
      <section
        v-for="status in visibleStatuses"
        :key="status"
        :aria-label="`${STATUS_LABELS[status]} column`"
        :class="cn(
          'flex min-w-0 flex-col rounded-xl border border-transparent bg-muted/50 p-2 transition-[opacity,background-color,border-color]',
          columnHint(status),
        )"
      >
        <header class="flex items-center justify-between px-1.5 pt-1 pb-2.5">
          <h2 class="flex items-center gap-2 text-sm font-semibold text-foreground">
            <span :class="cn('size-2 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
            {{ STATUS_LABELS[status] }}
          </h2>
          <Skeleton v-if="loading" class="h-5 w-7 rounded-full" />
          <span v-else class="rounded-full bg-card px-2 py-0.5 text-xs font-medium text-muted-foreground tabular-nums">
            {{ columns[status].total }}
          </span>
        </header>

        <div v-if="loading" class="flex flex-col gap-2" aria-hidden="true">
          <div v-for="n in 3" :key="n" class="rounded-xl border bg-card p-3">
            <Skeleton class="h-3.5 w-4/5" />
            <Skeleton class="mt-2 h-3 w-1/3" />
            <div class="mt-4 flex justify-between">
              <Skeleton class="size-6 rounded-md" />
              <Skeleton class="h-3 w-16" />
            </div>
          </div>
        </div>

        <div v-else class="relative flex flex-1 flex-col">
          <VueDraggable
            v-model="columns[status].items"
            group="board"
            :sort="false"
            :animation="150"
            :disabled="!isDesktop"
            filter=".is-pending"
            ghost-class="board-ghost"
            chosen-class="board-chosen"
            class="flex min-h-28 flex-1 flex-col gap-2"
            @start="onDragStart"
            @end="onDragEnd"
            @add="(e: DraggableEvent<ContentResponse>) => onDrop(status, e)"
          >
            <BoardCard
              v-for="item in columns[status].items"
              :key="item.id"
              :item="item"
              :pending="isPending(item.id)"
              @open="openEdit(item)"
              @move="(target) => move(item, target)"
            />
          </VueDraggable>

          <!-- Outside the draggable list so Sortable doesn't treat it as a card. -->
          <p
            v-if="columns[status].items.length === 0"
            class="pointer-events-none absolute inset-0 flex items-center justify-center rounded-xl border border-dashed text-sm text-muted-foreground"
          >
            {{ EMPTY_COLUMN_TEXT[status] }}
          </p>

          <Button
            v-if="columns[status].hasNext"
            variant="ghost"
            class="mt-2 h-9 w-full text-muted-foreground"
            :disabled="columns[status].loadingMore"
            @click="loadMore(status)"
          >
            {{ columns[status].loadingMore
              ? 'Loading…'
              : `Load more (${columns[status].total - columns[status].items.length} more)` }}
          </Button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Where the card will land while dragging. */
:deep(.board-ghost) {
  opacity: 0.4;
  border-style: dashed;
}
:deep(.board-chosen) {
  border-color: var(--ring);
}
</style>
