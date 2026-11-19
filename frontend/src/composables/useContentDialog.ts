import { onBeforeUnmount, reactive } from 'vue'
import type { ContentResponse } from '@/api'

interface EditorState {
  /** "Yeni İçerik" dialog. */
  newOpen: boolean
  /** Preselected brand for new content (e.g. from a brand page). */
  clientId: number | null
  /** Edit sheet. */
  editOpen: boolean
  /** The item in the edit sheet; kept up to date by the sheet's own status changes. */
  content: ContentResponse | null
}

// One New dialog and one Edit sheet live in the app layout; any page can open them.
const state = reactive<EditorState>({ newOpen: false, clientId: null, editOpen: false, content: null })
const savedListeners = new Set<(item: ContentResponse) => void>()

export function useContentDialog() {
  function openNew(options: { clientId?: number } = {}) {
    state.clientId = options.clientId ?? null
    state.newOpen = true
  }

  /** Opens the right-side edit sheet for this item. */
  function openEdit(content: ContentResponse) {
    state.content = content
    state.editOpen = true
  }

  /**
   * Lets the page that is showing content refresh itself after a create, an edit, or a status
   * change made in the sheet (server-confirmed only). Unregisters on unmount.
   */
  function onSaved(listener: (item: ContentResponse) => void) {
    savedListeners.add(listener)
    onBeforeUnmount(() => savedListeners.delete(listener))
  }

  return { state, openNew, openEdit, onSaved }
}

export function notifyContentSaved(item: ContentResponse) {
  savedListeners.forEach((listener) => listener(item))
}
