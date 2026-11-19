import { onBeforeUnmount, reactive } from 'vue'
import type { ClientResponse } from '@/api'

// One New brand dialog lives in the app layout; the Brands page and the Overview open it.
const state = reactive({ open: false })
const savedListeners = new Set<(client: ClientResponse) => void>()

export function useClientDialog() {
  function open() {
    state.open = true
  }

  /** Lets the page showing brands refresh itself after a create. Unregisters on unmount. */
  function onSaved(listener: (client: ClientResponse) => void) {
    savedListeners.add(listener)
    onBeforeUnmount(() => savedListeners.delete(listener))
  }

  return { state, open, onSaved }
}

export function notifyClientSaved(client: ClientResponse) {
  savedListeners.forEach((listener) => listener(client))
}
