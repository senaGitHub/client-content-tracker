// TypeScript mirrors of the backend record DTOs (see backend/**/dto).
// Dates: LocalDate -> 'YYYY-MM-DD' string, Instant -> ISO-8601 string.

export type ContentType = 'SOCIAL_POST' | 'BLOG_POST' | 'WEB_PAGE'
export type ContentStatus = 'DRAFT' | 'IN_REVIEW' | 'PUBLISHED'
export type ActivityAction = 'CREATED' | 'UPDATED' | 'STATUS_CHANGED'

export interface StatusCounts {
  draft: number
  inReview: number
  published: number
  overdue: number
  total: number
}

/** The client as embedded in a content response; its cover doubles as the content thumbnail. */
export interface ClientRef {
  id: number
  name: string
  coverImageUrl: string | null
}

export interface ClientResponse {
  id: number
  name: string
  industry: string
  contactName: string
  contactEmail: string
  coverImageUrl: string | null
  createdAt: string
  counts: StatusCounts
}

export interface ClientRequest {
  name: string
  industry: string
  contactName: string
  contactEmail: string
  /** Optional; one of the bundled /images/covers/*.jpg files. */
  coverImageUrl?: string | null
}

export interface ContentResponse {
  id: number
  title: string
  type: ContentType
  channel: string | null
  status: ContentStatus
  dueDate: string
  notes: string | null
  client: ClientRef
  /** Computed on the server: dueDate < today and status != PUBLISHED. */
  overdue: boolean
  /** Statuses this item may move to next (from the backend's domain rule). */
  allowedTransitions: ContentStatus[]
  createdAt: string
  updatedAt: string
}

export interface ContentRequest {
  clientId: number
  title: string
  type: ContentType
  channel?: string | null
  dueDate: string
  notes?: string | null
}

export interface ContentFilters {
  status?: ContentStatus
  clientId?: number
  type?: ContentType
  q?: string
  /** Past due and not published (same rule as the dashboard). */
  overdue?: boolean
  /** Unpublished and due today or in the next 6 days (same rule as the dashboard). */
  dueThisWeek?: boolean
}

/** Paging params for list endpoints. `page` is zero-based; `sort` is e.g. 'dueDate,asc'. */
export interface PageParams {
  page?: number
  size?: number
  sort?: string
}

/** Mirror of the backend's PageResponse record. */
export interface Page<T> {
  items: T[]
  /** Zero-based page index. */
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** Filters for GET /contents/status-counts: the list filters without status. */
export type StatusCountFilters = Omit<ContentFilters, 'status'>

/** One entry of a content's history, newest first. */
export interface ActivityResponse {
  id: number
  action: ActivityAction
  fromStatus: ContentStatus | null
  toStatus: ContentStatus | null
  createdAt: string
}

/** Real-data facts for the Overview. "This week" = the last 7 days, "last week" = the 7 before. */
export interface DashboardMetrics {
  createdThisWeek: number
  publishedThisWeek: number
  publishedLastWeek: number
  /** Null when nothing is in review. */
  oldestInReviewDays: number | null
  /** Null when nothing is overdue. */
  oldestOverdueDays: number | null
}

export interface DashboardResponse {
  totals: StatusCounts
  metrics: DashboardMetrics
  clients: ClientResponse[]
  /** The 4 longest-waiting items in review, longest first; the full count is totals.inReview. */
  approvals: ApprovalItem[]
}

/**
 * A content response plus when it entered review (ISO instant, from the activity log).
 * Optional: an older backend (e.g. mid-deploy) does not send it.
 */
export type ApprovalItem = ContentResponse & { inReviewSince?: string }
