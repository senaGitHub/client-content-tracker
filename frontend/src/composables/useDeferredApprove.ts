import { useEventListener } from '@vueuse/core'
import { computed, onBeforeUnmount, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import type { ContentResponse } from '@/api'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { statusChangeError } from '@/lib/errors'

/** How long "Geri al" stays possible before the PATCH is sent. */
export const APPROVE_DELAY_MS = 5000

interface PendingApproval {
  item: ContentResponse
  /** 'waiting' = still undoable; 'sending' = the PATCH is on its way. */
  phase: 'waiting' | 'sending'
  timer: ReturnType<typeof setTimeout>
  toastId: string | number
}

interface Options {
  /** Runs after the server accepted the approval; awaited so the item stays hidden until fresh data arrives. */
  onCommitted?: () => Promise<unknown> | void
  /** Runs when the server refused or could not be reached; the item is shown again. */
  onFailed?: () => void
}

/**
 * "Onayla" with a real undo. Publishing is final (the status rule has no way back from PUBLISHED),
 * so instead of sending the PATCH and undoing it, the PATCH waits ~5 s. "Geri al" cancels the timer
 * and nothing is ever sent. Leaving the page or closing the tab sends whatever is still waiting,
 * so an approval is never silently lost.
 */
export function useDeferredApprove(options: Options = {}) {
  const pending = reactive(new Map<number, PendingApproval>())
  let alive = true

  function approve(item: ContentResponse) {
    if (pending.has(item.id)) return
    const toastId = toast.success(`${item.title} yayına alınıyor`, {
      description: 'Birkaç saniye içinde yayınlanacak.',
      duration: APPROVE_DELAY_MS,
      action: { label: 'Geri al', onClick: () => cancel(item.id) },
    })
    const timer = setTimeout(() => void send(item.id), APPROVE_DELAY_MS)
    pending.set(item.id, { item, phase: 'waiting', timer, toastId })
  }

  function cancel(id: number) {
    const entry = pending.get(id)
    if (!entry || entry.phase !== 'waiting') return
    clearTimeout(entry.timer)
    pending.delete(id)
    toast(`${entry.item.title} tekrar Onayda durumunda`)
  }

  async function send(id: number, { keepalive = false } = {}) {
    const entry = pending.get(id)
    if (!entry || entry.phase !== 'waiting') return
    clearTimeout(entry.timer)
    entry.phase = 'sending'
    // A toast that is still open (e.g. paused by hovering) must not offer an undo that no longer works.
    toast.dismiss(entry.toastId)
    try {
      await contentsApi.changeStatus(id, 'PUBLISHED', { keepalive })
      void refreshNavCounts()
      if (alive) await options.onCommitted?.()
    } catch (e) {
      const { title, description } = statusChangeError(e)
      toast.error(title, { description: `${entry.item.title}: ${description}` })
      if (alive) options.onFailed?.()
    } finally {
      pending.delete(id)
    }
  }

  /** Sends every approval that is still waiting, right now. */
  function flush({ keepalive = false } = {}) {
    for (const id of [...pending.keys()]) void send(id, { keepalive })
  }

  // Closing the tab or navigating away from the site: keepalive lets the request finish.
  useEventListener(window, 'pagehide', () => flush({ keepalive: true }))
  // Leaving the Overview inside the app.
  onBeforeUnmount(() => {
    flush({ keepalive: true })
    alive = false
  })

  /** Items approved (waiting or sending) that the UI should already treat as published. */
  const pendingItems = computed(() => [...pending.values()].map((entry) => entry.item))

  return {
    approve,
    isPending: (id: number) => pending.has(id),
    pendingItems,
  }
}
