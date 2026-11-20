import { nextTick, reactive } from 'vue'
import { ApiError } from '@/api'
import type { ContentRequest, ContentResponse, ContentType } from '@/api'
import { fieldErrors } from '@/lib/errors'

export type ContentField = 'clientId' | 'title' | 'type' | 'channel' | 'dueDate' | 'notes'

// Order matters: the first invalid field in this order gets focus.
const FIELDS: ContentField[] = ['title', 'clientId', 'type', 'channel', 'dueDate', 'notes']

/**
 * The channels the Kanal field offers (ChannelSelect). Includes every channel the demo seeder uses
 * (Instagram, Facebook, LinkedIn, TikTok, YouTube, Web sitesi).
 */
export const CHANNEL_SUGGESTIONS = [
  'Instagram',
  'Facebook',
  'LinkedIn',
  'TikTok',
  'YouTube',
  'Web sitesi',
  'Blog',
  'E-bülten',
]

/**
 * Form state shared by the New content dialog and the Edit sheet. Status is deliberately not part
 * of it: the API's ContentRequest has no status (it only changes via PATCH /status), so saving the
 * form can never undo a status change made in the meantime.
 *
 * `idPrefix` keeps element ids unique while both forms are mounted.
 */
export function useContentForm(idPrefix: string) {
  const form = reactive({ clientId: '', title: '', type: '' as ContentType | '', channel: '', dueDate: '', notes: '' })
  const errors = reactive<Partial<Record<ContentField, string>>>({})
  let initial = ''

  function clearErrors() {
    FIELDS.forEach((f) => delete errors[f])
  }

  /** Fills the form from an item (edit) or with blanks (new). */
  function reset(from: ContentResponse | null, defaults: { clientId?: number | null } = {}) {
    form.clientId = from ? String(from.client.id) : defaults.clientId ? String(defaults.clientId) : ''
    form.title = from?.title ?? ''
    form.type = from?.type ?? ''
    form.channel = from?.channel ?? ''
    form.dueDate = from?.dueDate ?? ''
    form.notes = from?.notes ?? ''
    initial = JSON.stringify(form)
    clearErrors()
  }

  function isDirty() {
    return JSON.stringify(form) !== initial
  }

  function validate(): boolean {
    clearErrors()
    if (!form.title.trim()) errors.title = 'Başlık gerekli.'
    if (!form.clientId) errors.clientId = 'Bir marka seçin.'
    if (!form.type) errors.type = 'Bir tür seçin.'
    if (!form.dueDate) errors.dueDate = 'Teslim tarihi gerekli.'
    return Object.keys(errors).length === 0
  }

  function id(field: ContentField) {
    return `${idPrefix}-${field}`
  }

  async function focusFirstError() {
    await nextTick()
    const field = FIELDS.find((f) => errors[f])
    if (!field) return
    // The type picker is a group of buttons: focus the first one.
    const el = document.getElementById(id(field))
    const target = el?.matches('button, input, textarea, select') ? el : el?.querySelector<HTMLElement>('button')
    target?.focus()
  }

  /** True when the error was a 400 with field messages, now shown under the fields. */
  function applyServerErrors(error: unknown): boolean {
    if (!(error instanceof ApiError) || error.status !== 400 || !Object.keys(error.fieldErrors).length) return false
    Object.assign(errors, fieldErrors(error))
    void focusFirstError()
    return true
  }

  function body(): ContentRequest {
    return {
      clientId: Number(form.clientId),
      title: form.title.trim(),
      type: form.type as ContentType,
      channel: form.channel.trim() || null,
      dueDate: form.dueDate,
      notes: form.notes.trim() || null,
    }
  }

  function describedBy(field: ContentField) {
    return errors[field] ? `${id(field)}-error` : undefined
  }

  /**
   * Enter submits from any field (native form behaviour); in a textarea Enter adds a new line and
   * Ctrl/Cmd+Enter submits.
   */
  function onKeydown(event: KeyboardEvent, submit: () => void) {
    if (event.key !== 'Enter') return
    if (event.target instanceof HTMLTextAreaElement && (event.ctrlKey || event.metaKey)) {
      event.preventDefault()
      submit()
    }
  }

  return { form, errors, reset, isDirty, validate, focusFirstError, applyServerErrors, body, id, describedBy, onKeydown }
}
