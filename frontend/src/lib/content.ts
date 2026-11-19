import type { RouteLocationRaw } from 'vue-router'
import type { ContentFilters, ContentStatus, ContentType } from '@/api'

export const STATUS_ORDER: ContentStatus[] = ['DRAFT', 'IN_REVIEW', 'PUBLISHED']

export const STATUS_LABELS: Record<ContentStatus, string> = {
  DRAFT: 'Draft',
  IN_REVIEW: 'In review',
  PUBLISHED: 'Published',
}

/** Tailwind classes backed by the status tokens in main.css. */
export const STATUS_STYLES: Record<ContentStatus, { badge: string; dot: string }> = {
  DRAFT: { badge: 'bg-draft text-draft-foreground', dot: 'bg-draft-dot' },
  IN_REVIEW: { badge: 'bg-review text-review-foreground', dot: 'bg-review-dot' },
  PUBLISHED: { badge: 'bg-published text-published-foreground', dot: 'bg-published-dot' },
}

export const TYPE_LABELS: Record<ContentType, string> = {
  SOCIAL_POST: 'Social post',
  BLOG_POST: 'Blog post',
  WEB_PAGE: 'Web page',
}

/**
 * Link to the Content page with filters applied. The Content page (Phase 7) reads the same
 * names from the query string, matching the GET /api/contents parameters.
 */
export function contentLink(filters: ContentFilters = {}): RouteLocationRaw {
  const query: Record<string, string> = {}
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '' && value !== false) query[key] = String(value)
  }
  return { path: '/content', query }
}

export function clientLink(id: number): RouteLocationRaw {
  return { name: 'client-detail', params: { id: String(id) } }
}
