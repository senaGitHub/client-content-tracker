import {
  differenceInCalendarDays,
  format,
  formatDistanceToNow,
  isToday,
  parseISO,
  startOfDay,
} from 'date-fns'

/**
 * Parses a backend LocalDate ('YYYY-MM-DD') as a local calendar day.
 * `new Date('2026-11-19')` would be UTC midnight, which is the previous day west of UTC;
 * date-fns' parseISO treats a date-only string as local time.
 */
export function parseDay(value: string): Date {
  return parseISO(value)
}

/** "Thursday, 19 November" */
export function formatLongDate(date: Date = new Date()): string {
  return format(date, 'EEEE, d MMMM')
}

/** "19 Nov" */
export function formatShortDate(value: string): string {
  return format(parseDay(value), 'd MMM')
}

/** "19 Nov 2026, 14:05": full timestamp for tooltips. */
export function formatDateTime(value: string): string {
  return format(parseISO(value), 'd MMM yyyy, HH:mm')
}

/** "2 hours ago" */
export function formatRelative(value: string): string {
  return formatDistanceToNow(parseISO(value), { addSuffix: true })
}

/** Days between today and the due date: negative = in the past. */
export function daysUntil(dueDate: string, today: Date = new Date()): number {
  return differenceInCalendarDays(parseDay(dueDate), startOfDay(today))
}

/**
 * Human due-date label for a content item: "3 days overdue", "Due today", "Due tomorrow",
 * "Due Fri" (within the week), otherwise "Due 12 Dec". `overdue` comes from the server so the
 * rule (past due and not published) is not re-implemented here.
 */
export function dueLabel(item: { dueDate: string; overdue: boolean }, today: Date = new Date()): string {
  const days = daysUntil(item.dueDate, today)
  if (item.overdue) {
    const late = Math.max(1, -days)
    return `${late} ${late === 1 ? 'day' : 'days'} overdue`
  }
  if (days === 0) return 'Due today'
  if (days === 1) return 'Due tomorrow'
  if (days > 1 && days < 7) return `Due ${format(parseDay(item.dueDate), 'EEE')}`
  return `Due ${formatShortDate(item.dueDate)}`
}

export function isTodayDay(value: string): boolean {
  return isToday(parseDay(value))
}

export function pluralize(count: number, singular: string, plural = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`
}
