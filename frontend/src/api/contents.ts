import { http } from './http'
import type { ContentFilters, ContentRequest, ContentResponse, ContentStatus } from './types'

export const contentsApi = {
  list: (filters: ContentFilters = {}, signal?: AbortSignal) =>
    http.get<ContentResponse[]>('/contents', { query: { ...filters }, signal }),
  create: (body: ContentRequest) => http.post<ContentResponse>('/contents', body),
  update: (id: number, body: ContentRequest) => http.put<ContentResponse>(`/contents/${id}`, body),
  /** Rejects with ApiError status 409 when the transition is not allowed. */
  changeStatus: (id: number, status: ContentStatus) =>
    http.patch<ContentResponse>(`/contents/${id}/status`, { status }),
}
