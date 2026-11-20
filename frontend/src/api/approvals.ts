import { http } from './http'
import type { ApprovalItem, Page } from './types'

/**
 * Order of the approvals queue, as property,direction. "waiting" is time in review, so
 * 'waiting,desc' (the default) is longest-waiting first. Ties are broken by id on the server.
 */
export type ApprovalSort = 'waiting,desc' | 'waiting,asc'

/** Filters for GET /approvals. */
export interface ApprovalFilters {
  clientId?: number
  /** Omitted = 'waiting,desc'. */
  sort?: ApprovalSort
}

export const approvalsApi = {
  /**
   * Content in review, paged. Size defaults to 20 on the server (max 100). Unfiltered,
   * totalElements equals the dashboard's totals.inReview. An unknown sort is a 400.
   */
  list: (filters: ApprovalFilters = {}, paging: { page?: number; size?: number } = {}, signal?: AbortSignal) =>
    http.get<Page<ApprovalItem>>('/approvals', { query: { ...filters, ...paging }, signal }),
}
