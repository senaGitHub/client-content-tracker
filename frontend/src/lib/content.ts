import type { RouteLocationRaw } from 'vue-router'
import type { ContentFilters, ContentStatus, ContentType } from '@/api'

export const STATUS_ORDER: ContentStatus[] = ['DRAFT', 'IN_REVIEW', 'PUBLISHED']

export const STATUS_LABELS: Record<ContentStatus, string> = {
  DRAFT: 'Taslak',
  IN_REVIEW: 'Onayda',
  PUBLISHED: 'Yayında',
}

/** Tailwind classes backed by the status tokens in main.css. */
export const STATUS_STYLES: Record<ContentStatus, { badge: string; dot: string; text: string }> = {
  DRAFT: { badge: 'bg-draft text-draft-foreground', dot: 'bg-draft-dot', text: 'text-draft-foreground' },
  IN_REVIEW: { badge: 'bg-review text-review-foreground', dot: 'bg-review-dot', text: 'text-review-foreground' },
  PUBLISHED: { badge: 'bg-published text-published-foreground', dot: 'bg-published-dot', text: 'text-published-foreground' },
}

/** What moving to a status is called in menus. */
export const TRANSITION_LABELS: Record<ContentStatus, string> = {
  DRAFT: 'Taslağa geri gönder',
  IN_REVIEW: 'Onaya gönder',
  PUBLISHED: 'Yayına al',
}

export const TYPE_LABELS: Record<ContentType, string> = {
  SOCIAL_POST: 'Sosyal Medya',
  BLOG_POST: 'Blog Yazısı',
  WEB_PAGE: 'Web Sayfası',
}

export const TYPE_ORDER: ContentType[] = ['SOCIAL_POST', 'BLOG_POST', 'WEB_PAGE']

/**
 * The "Teslim" filter (Content page toolbar and the Overview feed): 'overdue' maps to ?overdue=true,
 * 'week' to ?dueThisWeek=true. "Tümü" is no filter. The window is today and the next 6 days, not the
 * calendar week, so the label names the 7 days.
 */
export const DUE_OPTIONS = [
  { value: 'overdue', label: 'Geciken' },
  { value: 'week', label: 'Yaklaşan (7 gün)' },
]

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
