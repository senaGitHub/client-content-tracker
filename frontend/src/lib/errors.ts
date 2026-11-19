import { ApiError } from '@/api'
import type { ContentStatus } from '@/api'
import { STATUS_LABELS } from '@/lib/content'

/*
 * The API speaks English (ProblemDetail titles, details and field messages); the UI speaks Turkish.
 * This is the one place that turns an error into Turkish text, so components never show e.message.
 */

const FIELD_LABELS: Record<string, string> = {
  // Content
  clientId: 'Marka',
  title: 'Başlık',
  type: 'Tür',
  channel: 'Kanal',
  dueDate: 'Teslim tarihi',
  notes: 'Notlar',
  status: 'Durum',
  // Client
  name: 'Marka adı',
  industry: 'Sektör',
  contactName: 'İlgili kişi',
  contactEmail: 'E-posta',
  coverImageUrl: 'Kapak görseli',
}

/** Fields picked from a list, where "required" reads better as "choose one". */
const PICKER_FIELDS: Record<string, string> = {
  clientId: 'Bir marka seçin.',
  type: 'Bir tür seçin.',
  dueDate: 'Bir teslim tarihi seçin.',
  status: 'Bir durum seçin.',
}

/**
 * Translates one backend Bean Validation message by its shape, not word for word, so a new
 * field only needs a label above: "X is required.", "X must be at most N characters.",
 * "X must be a valid email address.".
 */
function translateFieldMessage(field: string, message: string): string {
  const label = FIELD_LABELS[field] ?? 'Bu alan'
  if (/is required/i.test(message)) return PICKER_FIELDS[field] ?? `${label} zorunludur.`
  const max = message.match(/at most (\d+) characters/i)
  if (max) return `${label} en fazla ${max[1]} karakter olabilir.`
  if (/valid email/i.test(message)) return 'Geçerli bir e-posta adresi girin.'
  return `${label} geçersiz.`
}

/** Field name → Turkish message, for 400 validation errors (empty for anything else). */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {}
  return Object.fromEntries(
    Object.entries(error.fieldErrors).map(([field, message]) => [field, translateFieldMessage(field, message)]),
  )
}

/** One Turkish sentence describing any error, for toasts and inline error states. */
export function errorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return 'Bir şeyler ters gitti. Lütfen tekrar deneyin.'
  if (error.status === 0) return 'Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.'
  if (error.status === 409) return statusChangeMessage(error)
  if (error.status === 404) return 'Aradığınız kayıt bulunamadı. Silinmiş ya da taşınmış olabilir.'
  if (error.status === 400) {
    return Object.keys(error.fieldErrors).length
      ? 'Bazı alanlar eksik ya da hatalı. Lütfen kontrol edin.'
      : 'İstek geçersiz. Lütfen bilgileri kontrol edin.'
  }
  if (error.status >= 500) return 'Sunucuda bir sorun oluştu. Lütfen biraz sonra tekrar deneyin.'
  return 'Bir şeyler ters gitti. Lütfen tekrar deneyin.'
}

/** Why a status move was refused (409), using the statuses the backend sends with the problem. */
function statusChangeMessage(error: ApiError): string {
  const from = error.problem.currentStatus as ContentStatus | undefined
  const to = error.problem.requestedStatus as ContentStatus | undefined
  if (from === 'PUBLISHED') return 'Yayındaki içeriğin durumu değiştirilemez.'
  if (from === 'DRAFT' && to === 'PUBLISHED') return 'Taslaktaki içerik onaya gönderilmeden yayınlanamaz.'
  if (from && to && STATUS_LABELS[from] && STATUS_LABELS[to]) {
    return `${STATUS_LABELS[from]} durumundaki içerik ${STATUS_LABELS[to]} durumuna taşınamaz.`
  }
  return 'Bu durum değişikliğine izin verilmiyor.'
}

/** Title + description for a failed status change toast. */
export function statusChangeError(error: unknown): { title: string; description: string } {
  if (error instanceof ApiError && error.status === 409) {
    return { title: 'Bu geçiş yapılamaz', description: statusChangeMessage(error) }
  }
  return { title: 'Durum değiştirilemedi', description: errorMessage(error) }
}
