import { onBeforeUnmount, reactive } from 'vue'
import type { ClientResponse } from '@/api'

// One brand dialog lives in the app layout: the Brands page opens it to create a brand, the brand
// detail page to edit one. `client` is the brand being edited, or null when creating.
const state = reactive<{ open: boolean; client: ClientResponse | null }>({ open: false, client: null })
const savedListeners = new Set<(client: ClientResponse) => void>()

export function useClientDialog() {
  /** Opens the dialog empty, to create a brand. */
  function open() {
    state.client = null
    state.open = true
  }

  /** Opens the dialog filled with this brand, to edit it. */
  function openEdit(client: ClientResponse) {
    state.client = client
    state.open = true
  }

  /** Lets the page showing brands refresh itself after a create or an edit. Unregisters on unmount. */
  function onSaved(listener: (client: ClientResponse) => void) {
    savedListeners.add(listener)
    onBeforeUnmount(() => savedListeners.delete(listener))
  }

  return { state, open, openEdit, onSaved }
}

export function notifyClientSaved(client: ClientResponse) {
  savedListeners.forEach((listener) => listener(client))
}
