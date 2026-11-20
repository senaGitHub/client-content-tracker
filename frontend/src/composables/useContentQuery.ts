import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQuery, LocationQueryRaw } from 'vue-router'
import type { ContentFilters, ContentStatus, ContentType } from '@/api'
import { STATUS_ORDER, TYPE_LABELS } from '@/lib/content'

export type ContentView = 'board' | 'list'
export type DueFilter = 'all' | 'overdue' | 'week'

/** Everything the Content page shows is described by the URL, so links, reloads and Back just work. */
export interface ContentQuery {
  view: ContentView
  q: string
  clientId?: number
  type?: ContentType
  /** List view only; the board shows every status as a column. */
  status?: ContentStatus
  due: DueFilter
  /** One-based, as shown to people. */
  page: number
  /** e.g. 'title,asc'; undefined = the list's default, 'priority' (see ContentList). */
  sort?: string
}

const VIEW_STORAGE_KEY = 'harbour-content-view'
const TYPES = Object.keys(TYPE_LABELS) as ContentType[]

function first(value: LocationQuery[string]): string | undefined {
  const v = Array.isArray(value) ? value[0] : value
  return v ?? undefined
}

/** The view last picked on this device; List when nothing was picked yet (or storage is blocked). */
function savedView(): ContentView {
  try {
    return localStorage.getItem(VIEW_STORAGE_KEY) === 'board' ? 'board' : 'list'
  } catch {
    return 'list'
  }
}

function parse(query: LocationQuery): ContentQuery {
  const view = first(query.view)
  const clientId = Number(first(query.clientId))
  const type = first(query.type) as ContentType | undefined
  const status = first(query.status) as ContentStatus | undefined
  const page = Number(first(query.page))
  return {
    view: view === 'list' || view === 'board' ? view : savedView(),
    q: first(query.q) ?? '',
    clientId: Number.isInteger(clientId) && clientId > 0 ? clientId : undefined,
    type: type && TYPES.includes(type) ? type : undefined,
    status: status && STATUS_ORDER.includes(status) ? status : undefined,
    // Same parameter names as the API and the Overview links.
    due: first(query.overdue) === 'true' ? 'overdue' : first(query.dueThisWeek) === 'true' ? 'week' : 'all',
    page: Number.isInteger(page) && page > 1 ? page : 1,
    sort: first(query.sort) || undefined,
  }
}

function serialize(state: ContentQuery): LocationQueryRaw {
  const query: LocationQueryRaw = { view: state.view }
  if (state.q.trim()) query.q = state.q.trim()
  if (state.clientId) query.clientId = String(state.clientId)
  if (state.type) query.type = state.type
  if (state.status) query.status = state.status
  if (state.due === 'overdue') query.overdue = 'true'
  if (state.due === 'week') query.dueThisWeek = 'true'
  if (state.page > 1) query.page = String(state.page)
  if (state.sort) query.sort = state.sort
  return query
}

export function useContentQuery() {
  const route = useRoute()
  const router = useRouter()

  const state = computed(() => parse(route.query))

  /** API filters shared by both views (status is added per column / in the list). */
  const filters = computed<ContentFilters>(() => ({
    q: state.value.q.trim() || undefined,
    clientId: state.value.clientId,
    type: state.value.type,
    overdue: state.value.due === 'overdue',
    dueThisWeek: state.value.due === 'week',
  }))

  const hasFilters = computed(
    () => !!(state.value.q.trim() || state.value.clientId || state.value.type || state.value.status || state.value.due !== 'all'),
  )

  /** Any filter change goes back to page 1; paging and sorting pass `keepPage`. */
  function update(patch: Partial<ContentQuery>, { keepPage = false } = {}) {
    const next = { ...state.value, ...patch }
    if (!keepPage && !('page' in patch)) next.page = 1
    if (patch.view) {
      try {
        localStorage.setItem(VIEW_STORAGE_KEY, patch.view)
      } catch {
        // Not critical: the view is also in the URL.
      }
    }
    router.replace({ query: serialize(next) })
  }

  function clearFilters() {
    update({ q: '', clientId: undefined, type: undefined, status: undefined, due: 'all' })
  }

  return { state, filters, hasFilters, update, clearFilters }
}
