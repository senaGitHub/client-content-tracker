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
  /** Runs right after "Onayla", before anything is sent: show the item as published. */
  onStart?: (item: ContentResponse) => void
  /** "Geri al" was used in time: nothing was sent, show the item as it was. */
  onCancel?: (item: ContentResponse) => void
  /** The server accepted; awaited so callers can keep the item hidden until fresh data arrives. */
  onCommitted?: (updated: ContentResponse) => Promise<unknown> | void
  /** The server refused or could not be reached: show the item as it was. */
  onFailed?: (item: ContentResponse) => void
  /** Appended to the error toast, e.g. where the item went back to. */
  failureHint?: string
}

/**
 * Publishing with a real undo. Publishing is final (the status rule has no way back from
 * PUBLISHED), so instead of sending the PATCH and undoing it, the PATCH waits ~5 s. "Geri al"
 * cancels the timer and nothing is ever sent. Leaving the page (unmount) or closing the tab sends
 * whatever is still waiting, so an approval is never silently lost.
 *
 * Every publish in the app goes through here: directly for the Overview approvals panel, and via
 * useStatusChange for the board, the list, the feed and the edit sheet.
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
    options.onStart?.(item)
  }

  function cancel(id: number) {
    const entry = pending.get(id)
    if (!entry || entry.phase !== 'waiting') return
    clearTimeout(entry.timer)
    pending.delete(id)
    toast(`${entry.item.title} tekrar Onayda durumunda`)
    if (alive) options.onCancel?.(entry.item)
  }

  async function send(id: number, { keepalive = false } = {}) {
    const entry = pending.get(id)
    if (!entry || entry.phase !== 'waiting') return
    clearTimeout(entry.timer)
    entry.phase = 'sending'
    // A toast that is still open (e.g. paused by hovering) must not offer an undo that no longer works.
    toast.dismiss(entry.toastId)
    try {
      const updated = await contentsApi.changeStatus(id, 'PUBLISHED', { keepalive })
      // Fresh counts (the sidebar badge) and fresh views arrive together; only then does the item stop
      // counting as pending (finally), so nothing flips back to the old state for a moment.
      await Promise.all([refreshNavCounts(), alive ? options.onCommitted?.(updated) : undefined])
    } catch (e) {
      const { title, description } = statusChangeError(e)
      const hint = options.failureHint ? ` ${options.failureHint}` : ''
      toast.error(title, { description: `${entry.item.title}: ${description}${hint}` })
      if (alive) options.onFailed?.(entry.item)
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
  // Leaving the view inside the app.
  onBeforeUnmount(() => {
    flush({ keepalive: true })
    alive = false
  })

  /** Items approved (waiting or sending) that the UI should already treat as published. */
  const pendingItems = computed(() => [...pending.values()].map((entry) => entry.item))

  return {
    approve,
    /** The same undo as the toast's "Geri al": only while the PATCH has not been sent. */
    cancel,
    flush,
    isPending: (id: number) => pending.has(id),
    /** True while "Geri al" still works (waiting, not yet sending). */
    canCancel: (id: number) => pending.get(id)?.phase === 'waiting',
    pendingItems,
  }
}
