import { http } from './http'
import type { WriteOptions } from './http'
import type {
  ActivityResponse,
  ContentFilters,
  ContentRequest,
  ContentResponse,
  ContentStatus,
  Page,
  PageParams,
  StatusCountFilters,
  StatusCounts,
} from './types'

export const contentsApi = {
  /** Paged; sorted by due date unless `sort` is given. Size defaults to 20 on the server (max 100). */
  list: (filters: ContentFilters = {}, paging: PageParams = {}, signal?: AbortSignal) =>
    http.get<Page<ContentResponse>>('/contents', { query: { ...filters, ...paging }, signal }),
  /** Counts per status for the same filters (minus status): feeds the list's status tabs. */
  statusCounts: (filters: StatusCountFilters = {}, signal?: AbortSignal) =>
    http.get<StatusCounts>('/contents/status-counts', { query: { ...filters }, signal }),
  /** History, newest first. Rejects with ApiError 404 when the content doesn't exist. */
  activity: (id: number, signal?: AbortSignal) =>
    http.get<ActivityResponse[]>(`/contents/${id}/activity`, { signal }),
  create: (body: ContentRequest) => http.post<ContentResponse>('/contents', body),
  update: (id: number, body: ContentRequest) => http.put<ContentResponse>(`/contents/${id}`, body),
  /**
   * Rejects with ApiError status 409 when the transition is not allowed.
   * `keepalive` lets a deferred approval still reach the server while the tab is closing.
   */
  changeStatus: (id: number, status: ContentStatus, options?: WriteOptions) =>
    http.patch<ContentResponse>(`/contents/${id}/status`, { status }, options),
}
