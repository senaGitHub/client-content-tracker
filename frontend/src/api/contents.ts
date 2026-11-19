import { http } from './http'
import type { ContentFilters, ContentRequest, ContentResponse, ContentStatus, Page, PageParams } from './types'

export const contentsApi = {
  /** Paged; sorted by due date unless `sort` is given. Size defaults to 20 on the server (max 100). */
  list: (filters: ContentFilters = {}, paging: PageParams = {}, signal?: AbortSignal) =>
    http.get<Page<ContentResponse>>('/contents', { query: { ...filters, ...paging }, signal }),
  create: (body: ContentRequest) => http.post<ContentResponse>('/contents', body),
  update: (id: number, body: ContentRequest) => http.put<ContentResponse>(`/contents/${id}`, body),
  /** Rejects with ApiError status 409 when the transition is not allowed. */
  changeStatus: (id: number, status: ContentStatus) =>
    http.patch<ContentResponse>(`/contents/${id}/status`, { status }),
}
