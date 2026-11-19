import {
  differenceInCalendarDays,
  format,
  formatDistanceToNow,
  isToday,
  parseISO,
  startOfDay,
} from 'date-fns'
import { tr } from 'date-fns/locale'

// Every visible date goes through these helpers so the Turkish locale is applied in one place.

/**
 * Parses a backend LocalDate ('YYYY-MM-DD') as a local calendar day.
 * `new Date('2026-11-19')` would be UTC midnight, which is the previous day west of UTC;
 * date-fns' parseISO treats a date-only string as local time.
 */
export function parseDay(value: string): Date {
  return parseISO(value)
}

/** "19 Kasım, Perşembe" */
export function formatLongDate(date: Date = new Date()): string {
  return format(date, 'd MMMM, EEEE', { locale: tr })
}

/** "Per, 19 Kas" */
export function formatHeaderDate(date: Date = new Date()): string {
  return format(date, 'EEE, d MMM', { locale: tr })
}

/** "19 Kas" */
export function formatShortDate(value: string): string {
  return format(parseDay(value), 'd MMM', { locale: tr })
}

/** "17 Kas 2026" */
export function formatDate(value: string): string {
  return format(parseDay(value), 'd MMM yyyy', { locale: tr })
}

/** "19 Kas 2026, 14:05": full timestamp for tooltips and history. */
export function formatDateTime(value: string): string {
  return format(parseISO(value), 'd MMM yyyy, HH:mm', { locale: tr })
}

/** "2 saat önce" */
export function formatRelative(value: string): string {
  return formatDistanceToNow(parseISO(value), { addSuffix: true, locale: tr })
}

/** Days between today and the due date: negative = in the past. */
export function daysUntil(dueDate: string, today: Date = new Date()): number {
  return differenceInCalendarDays(parseDay(dueDate), startOfDay(today))
}

/** "2 gün gecikti" (at least 1 day). */
export function overdueLabel(dueDate: string, today: Date = new Date()): string {
  return `${Math.max(1, -daysUntil(dueDate, today))} gün gecikti`
}

/**
 * Due-date label for a content item: "2 gün gecikti", "Bugün", "Yarın", "Cuma" (within the week),
 * otherwise "12 Ara". `overdue` comes from the server so the rule (past due and not published)
 * is not re-implemented here.
 */
export function dueLabel(item: { dueDate: string; overdue: boolean }, today: Date = new Date()): string {
  if (item.overdue) return overdueLabel(item.dueDate, today)
  const days = daysUntil(item.dueDate, today)
  if (days === 0) return 'Bugün'
  if (days === 1) return 'Yarın'
  if (days > 1 && days < 7) return format(parseDay(item.dueDate), 'EEEE', { locale: tr })
  return formatShortDate(item.dueDate)
}

export function isTodayDay(value: string): boolean {
  return isToday(parseDay(value))
}
