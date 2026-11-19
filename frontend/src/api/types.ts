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

export interface ClientRef {
  id: number
  name: string
}

export interface ClientResponse {
  id: number
  name: string
  industry: string
  contactName: string
  contactEmail: string
  createdAt: string
  counts: StatusCounts
}

export interface ClientRequest {
  name: string
  industry: string
  contactName: string
  contactEmail: string
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
}

export interface ActivityResponse {
  id: number
  action: ActivityAction
  fromStatus: ContentStatus | null
  toStatus: ContentStatus | null
  createdAt: string
  content: { id: number; title: string }
  client: ClientRef
}

export interface NeedsAttention {
  overdue: ContentResponse[]
  dueThisWeek: ContentResponse[]
  awaitingApproval: ContentResponse[]
}

export interface WeekDay {
  date: string
  items: ContentResponse[]
}

export interface DashboardResponse {
  totals: StatusCounts
  needsAttention: NeedsAttention
  week: WeekDay[]
  clients: ClientResponse[]
  recentActivity: ActivityResponse[]
}
