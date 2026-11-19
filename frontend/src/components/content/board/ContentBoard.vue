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
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useContentDialog } from '@/composables/useContentDialog'
import { asPublishing, useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { STATUS_LABELS, STATUS_ORDER, STATUS_STYLES } from '@/lib/content'
import { errorMessage } from '@/lib/errors'
import { cn } from '@/lib/utils'
import BoardCard from './BoardCard.vue'

const PAGE_SIZE = 20

const props = defineProps<{ filters: ContentFilters; hasFilters: boolean }>()
const emit = defineEmits<{
  clear: []
  /** A status change was confirmed or rolled back by the server (status counts may differ). */
  changed: []
}>()

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

// ---- Moving cards --------------------------------------------------------------------------

function byDueDate(a: ContentResponse, b: ContentResponse) {
  return a.dueDate.localeCompare(b.dueDate) || a.id - b.id
}

/** Shows `item` in the column of its status, sorted by due date, and keeps totals in step. */
function show(item: ContentResponse) {
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

function place(item: ContentResponse, phase: PlacePhase) {
  show(item)
  if (phase !== 'optimistic') emit('changed')
}

// Publishing waits ~5 s before it is sent (see useStatusChange); other moves go at once.
const { move, isPending, publishingItems } = useStatusChange(place)

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
    // Publishes still waiting are not on the server yet: keep them in Yayında.
    publishingItems.value.forEach((item) => {
      if (statusById.has(item.id)) show(asPublishing(item))
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

// ---- Drag & drop ---------------------------------------------------------------------------

// While a card is dragged, columns it may go to say "Buraya bırak" and the others fade.
const dragging = ref<ContentResponse | null>(null)

type Hint = 'none' | 'allowed' | 'blocked'

function hintFor(status: ContentStatus): Hint {
  const item = dragging.value
  if (!item || item.status === status) return 'none'
  return item.allowedTransitions.includes(status) ? 'allowed' : 'blocked'
}

function onDragStart(event: DraggableEvent<ContentResponse>) {
  dragging.value = event.data
}

function onDragEnd() {
  dragging.value = null
}

/**
 * A card was dropped here. Even a column that is marked "Bu geçiş yapılamaz" accepts the drop:
 * the server decides, and a refused move (409) snaps back with an explanation.
 */
function onDrop(status: ContentStatus, event: DraggableEvent<ContentResponse>) {
  const item = event.data
  if (item && item.status !== status) void move(item, status)
}

// ---- Layout --------------------------------------------------------------------------------

// Three columns don't fit on a phone: show one at a time and move cards with the card menu.
const isDesktop = useMediaQuery('(min-width: 768px)')
const mobileColumn = ref<ContentStatus>('DRAFT')
const visibleStatuses = computed(() => (isDesktop.value ? STATUS_ORDER : [mobileColumn.value]))

const isEmpty = computed(() => !loading.value && !error.value && STATUS_ORDER.every((s) => columns[s].total === 0))

const EMPTY_COLUMN_TEXT: Record<ContentStatus, string> = {
  DRAFT: 'Taslak yok',
  IN_REVIEW: 'Onay bekleyen içerik yok',
  PUBLISHED: 'Henüz yayında içerik yok',
}
</script>

<template>
  <div
    v-if="error"
    role="alert"
    class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"
  >
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
      <span><span class="font-medium text-foreground">Pano yüklenemedi.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="loadAll">Tekrar dene</Button>
  </div>

  <EmptyState
    v-else-if="isEmpty && hasFilters"
    :icon="SearchXIcon"
    title="Bu filtrelere uyan içerik yok"
    description="Başka bir marka, tür ya da teslim tarihi deneyin veya filtreleri temizleyin."
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

  <div v-else class="flex flex-col gap-3">
    <!-- Mobile column switcher -->
    <div v-if="!isDesktop" role="tablist" aria-label="Pano sütunu" class="grid grid-cols-3 gap-1 rounded-lg bg-muted p-1">
      <button
        v-for="status in STATUS_ORDER"
        :key="status"
        type="button"
        role="tab"
        :aria-selected="mobileColumn === status"
        :class="cn(
          'flex h-9 items-center justify-center gap-1.5 rounded-md text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50',
          mobileColumn === status ? 'bg-card font-semibold text-foreground shadow-[0_1px_3px_rgba(0,0,0,0.08)]' : 'font-medium text-muted-foreground',
        )"
        @click="mobileColumn = status"
      >
        <span :class="cn('size-2 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
        <span class="truncate">{{ STATUS_LABELS[status] }}</span>
        <span v-if="!loading" class="text-xs text-muted-foreground tabular-nums">{{ columns[status].total }}</span>
      </button>
    </div>

    <div class="grid items-start gap-4 md:grid-cols-3" :aria-busy="loading">
      <section
        v-for="status in visibleStatuses"
        :key="status"
        :aria-label="`${STATUS_LABELS[status]} sütunu`"
        :class="cn(
          'flex min-w-0 flex-col gap-2.5 rounded-card border bg-column p-3.5 transition-[opacity,background-color,border-color,box-shadow]',
          hintFor(status) === 'allowed' && 'border-coral bg-coral-soft/50 ring-1 ring-coral',
          hintFor(status) === 'blocked' && 'opacity-55',
        )"
      >
        <header class="flex items-center gap-2 px-1 pt-0.5 pb-1">
          <span :class="cn('size-2.5 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
          <h2 class="text-base font-bold">{{ STATUS_LABELS[status] }}</h2>
          <Skeleton v-if="loading" class="h-4 w-6" />
          <span v-else class="text-[13px] font-semibold text-muted-foreground tabular-nums">{{ columns[status].total }}</span>
          <span v-if="hintFor(status) === 'allowed'" class="ml-auto text-xs font-semibold text-coral-text">Buraya bırak</span>
          <span v-else-if="hintFor(status) === 'blocked'" class="ml-auto text-xs text-muted-foreground">Bu geçiş yapılamaz</span>
        </header>

        <div v-if="loading" class="flex flex-col gap-2.5" aria-hidden="true">
          <div v-for="n in 3" :key="n" class="flex flex-col gap-2.5 rounded-2xl border bg-card p-3">
            <div class="flex gap-2.5">
              <Skeleton class="h-[46px] w-[62px] rounded-[9px]" />
              <div class="flex-1 space-y-2 pt-1">
                <Skeleton class="h-3.5 w-4/5" />
                <Skeleton class="h-3 w-1/2" />
              </div>
            </div>
            <div class="flex justify-between">
              <Skeleton class="h-3 w-24" />
              <Skeleton class="h-3 w-12" />
            </div>
          </div>
        </div>

        <div v-else class="relative flex flex-1 flex-col gap-2.5">
          <VueDraggable
            v-model="columns[status].items"
            group="board"
            :sort="false"
            :animation="150"
            :disabled="!isDesktop"
            :force-fallback="true"
            :fallback-on-body="true"
            filter=".is-pending"
            :prevent-on-filter="false"
            ghost-class="board-ghost"
            fallback-class="board-drag"
            class="flex min-h-28 flex-1 flex-col gap-2.5"
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
            class="pointer-events-none absolute inset-x-0 top-0 flex h-28 items-center justify-center rounded-2xl border border-dashed border-input text-sm text-muted-foreground"
          >
            {{ EMPTY_COLUMN_TEXT[status] }}
          </p>

          <Button
            v-if="columns[status].hasNext"
            variant="outline"
            class="w-full"
            :disabled="columns[status].loadingMore"
            @click="loadMore(status)"
          >
            {{ columns[status].loadingMore
              ? 'Yükleniyor…'
              : `Daha fazla yükle (${columns[status].total - columns[status].items.length})` }}
          </Button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Where the card will land: a dashed coral slot. */
:deep(.board-ghost) {
  border: 2px dashed var(--coral);
  background: var(--coral-soft);
  box-shadow: none;
}
:deep(.board-ghost > *) {
  visibility: hidden;
}
</style>

<style>
/*
 * The card under the pointer (Sortable's fallback clone lives on <body>, so this is unscoped).
 * `rotate` is its own property, so it adds to the translate Sortable writes into `transform`.
 */
.board-drag {
  rotate: -3deg;
  border-color: var(--coral) !important;
  box-shadow: 0 18px 40px rgba(60, 30, 20, 0.22) !important;
  opacity: 1 !important;
  cursor: grabbing;
}
@media (prefers-reduced-motion: reduce) {
  .board-drag {
    rotate: none;
  }
}
</style>
