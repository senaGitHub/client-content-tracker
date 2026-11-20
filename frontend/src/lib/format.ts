import {
  differenceInCalendarDays,
  differenceInMinutes,
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

/** "20 Kasım 2026, Cuma" */
export function formatLongDate(date: Date = new Date()): string {
  return format(date, 'd MMMM yyyy, EEEE', { locale: tr })
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

/**
 * "9 gündür bekliyor", or "bugün gönderildi" when it entered review today (calendar days).
 * Undefined when `since` is missing or not a valid date, e.g. an older backend during a deploy.
 */
export function waitingLabel(since: string | undefined, today: Date = new Date()): string | undefined {
  if (!since) return undefined
  const days = differenceInCalendarDays(startOfDay(today), parseISO(since))
  if (Number.isNaN(days)) return undefined
  return days <= 0 ? 'bugün gönderildi' : `${days} gündür bekliyor`
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
 * Due-date label for a content item: "2 gün gecikti", "Bugün", "Yarın", "3 gün kaldı" (2–6 days),
 * otherwise "12 Ara". The one place for this rule: the Overview feed, the content list (also on the
 * brand detail page) and board cards all use it, directly or through `dueHint`. `overdue` comes from the server so the rule (past due and not published)
 * is not re-implemented here.
 */
export function dueLabel(item: { dueDate: string; overdue: boolean }, today: Date = new Date()): string {
  if (item.overdue) return overdueLabel(item.dueDate, today)
  const days = daysUntil(item.dueDate, today)
  if (days === 0) return 'Bugün'
  if (days === 1) return 'Yarın'
  if (days > 1 && days < 7) return `${days} gün kaldı`
  return formatShortDate(item.dueDate)
}

/**
 * Second line under a due date: "2 gün gecikti" when overdue, or "Bugün" / "Yarın" / "N gün kaldı" when
 * it is close and not published yet; undefined otherwise.
 */
export function dueHint(item: { dueDate: string; overdue: boolean; status: string }): string | undefined {
  if (item.overdue) return dueLabel(item)
  if (item.status === 'PUBLISHED') return undefined
  const days = daysUntil(item.dueDate)
  return days >= 0 && days < 7 ? dueLabel(item) : undefined
}

/** Compact "last updated": "az önce", "5 dk önce", "2 sa önce", "dün", "3 gün önce", then "12 Kas". */
export function formatUpdated(value: string, now: Date = new Date()): string {
  const date = parseISO(value)
  const minutes = differenceInMinutes(now, date)
  if (minutes < 1) return 'az önce'
  if (minutes < 60) return `${minutes} dk önce`
  const days = differenceInCalendarDays(startOfDay(now), startOfDay(date))
  if (days === 0) return `${Math.floor(minutes / 60)} sa önce`
  if (days === 1) return 'dün'
  if (days < 7) return `${days} gün önce`
  return format(date, 'd MMM', { locale: tr })
}

export function isTodayDay(value: string): boolean {
  return isToday(parseDay(value))
}

// Ablative suffix ("-dan/-den/-tan/-ten") by how the number's last spoken word ends.
const ONES_SUFFIX = ['', "'den", "'den", "'ten", "'ten", "'ten", "'dan", "'den", "'den", "'dan"] // bir … dokuz
const TENS_SUFFIX = ['', "'dan", "'den", "'dan", "'tan", "'den", "'tan", "'ten", "'den", "'dan"] // on … doksan

function ablative(n: number): string {
  if (n % 10) return ONES_SUFFIX[n % 10]!
  if (n % 100) return TENS_SUFFIX[(n % 100) / 10]!
  return "'den" // yüz, bin
}

/** "1 Eylül 2026'dan beri" (the year carries the suffix). */
export function formatSince(value: string): string {
  const date = parseISO(value)
  return `${format(date, 'd MMMM yyyy', { locale: tr })}${ablative(date.getFullYear())} beri`
}

/** A content title as status toasts show it: “Yaz kampanyası”. */
export function quoted(title: string): string {
  return `“${title}”`
}
