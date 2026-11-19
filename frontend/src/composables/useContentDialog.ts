import { onBeforeUnmount, reactive } from 'vue'
import type { ContentResponse } from '@/api'

interface DialogState {
  open: boolean
  /** Set when editing; null for "New content". */
  content: ContentResponse | null
  /** Preselected client for new content (e.g. from a client page). */
  clientId: number | null
}

// A single dialog instance lives in the app layout; any page can open it.
const state = reactive<DialogState>({ open: false, content: null, clientId: null })
const savedListeners = new Set<(item: ContentResponse) => void>()

export function useContentDialog() {
  function openNew(options: { clientId?: number } = {}) {
    state.content = null
    state.clientId = options.clientId ?? null
    state.open = true
  }

  function openEdit(content: ContentResponse) {
    state.content = content
    state.clientId = null
    state.open = true
  }

  /** Lets the page that is showing content refresh itself after a save. Unregisters on unmount. */
  function onSaved(listener: (item: ContentResponse) => void) {
    savedListeners.add(listener)
    onBeforeUnmount(() => savedListeners.delete(listener))
  }

  return { state, openNew, openEdit, onSaved }
}

export function notifyContentSaved(item: ContentResponse) {
  savedListeners.forEach((listener) => listener(item))
}
