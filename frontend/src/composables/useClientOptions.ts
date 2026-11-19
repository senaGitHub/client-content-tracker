import { ref } from 'vue'
import { clientsApi } from '@/api'
import type { ClientRef } from '@/api'

// Shared across components: filters and the content dialog need the same short list.
const options = ref<ClientRef[]>([])
const loading = ref(false)
let request: Promise<void> | null = null

/** All clients as { id, name }, loaded once (the API caps a page at 100, plenty for an agency). */
export function useClientOptions() {
  function load(force = false) {
    if (request && !force) return request
    loading.value = true
    request = clientsApi
      .list({ size: 100 })
      .then((page) => {
        options.value = page.items.map(({ id, name }) => ({ id, name }))
      })
      .catch(() => {
        request = null // allow a retry next time
      })
      .finally(() => {
        loading.value = false
      })
    return request
  }

  load()
  return { clients: options, loading, reload: () => load(true) }
}
