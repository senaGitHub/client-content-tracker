import { useEventListener } from '@vueuse/core'
import { computed, onBeforeUnmount, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import type { ContentResponse, ContentStatus } from '@/api'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { quoted } from '@/lib/format'
import { statusChangeError } from '@/lib/errors'

/**
 * How long "Geri al" stays possible before the PATCH is sent. Also the duration of every status toast
 * that offers "Geri al" (useStatusChange), so they all last the same.
 */
export const DEFER_DELAY_MS = 5000

interface PendingMove {
  item: ContentResponse
  target: ContentStatus
  /** 'waiting' = still undoable; 'sending' = the PATCH is on its way. */
  phase: 'waiting' | 'sending'
  timer: ReturnType<typeof setTimeout>
  toastId: string | number
}

interface Options {
  /** Runs right after the move is started, before anything is sent. */
  onStart?: (item: ContentResponse, target: ContentStatus) => void
  /** "Geri al" was used in time: nothing was sent, show the item as it was. */
  onCancel?: (item: ContentResponse) => void
  /** The server accepted; awaited so callers can keep the item pending until fresh data arrives. */
  onCommitted?: (updated: ContentResponse) => Promise<unknown> | void
  /** The server refused or could not be reached: show the item as it was. */
  onFailed?: (item: ContentResponse) => void
  /** Appended to the error toast, e.g. where the item went back to. */
  failureHint?: string
  /** Overrides the waiting toast's text, e.g. the approvals queues' "“…” taslağa döndürülüyor". */
  toastText?: (item: ContentResponse, target: ContentStatus) => { title: string; description: string }
}

/** Default toast text while a publish waits (the content page, board, feed and edit sheet). */
function waitingMessage(title: string): { title: string; description: string } {
  return { title: `${quoted(title)} yayına alınıyor`, description: 'Birkaç saniye içinde yayınlanacak.' }
}

/**
 * A status change with a real undo: the PATCH waits ~5 s, "Geri al" cancels the timer and nothing
 * is ever sent. Leaving the page (unmount) or closing the tab sends whatever is still waiting, so a
 * move is never silently lost.
 *
 * Used for every publish in the app (publishing is final: the status rule has no way back from
 * PUBLISHED), directly in the approvals queues and via useStatusChange for the board, the list, the
 * feed and the edit sheet. The approvals queues defer their revisions (IN_REVIEW → DRAFT) the same way.
 */
export function useDeferredMove(options: Options = {}) {
  const pending = reactive(new Map<number, PendingMove>())
  let alive = true

  function move(item: ContentResponse, target: ContentStatus) {
    if (pending.has(item.id)) return
    const message = options.toastText?.(item, target) ?? waitingMessage(item.title)
    const toastId = toast.success(message.title, {
      description: message.description,
      duration: DEFER_DELAY_MS,
      action: { label: 'Geri al', onClick: () => cancel(item.id) },
    })
    const timer = setTimeout(() => void send(item.id), DEFER_DELAY_MS)
    pending.set(item.id, { item, target, phase: 'waiting', timer, toastId })
    options.onStart?.(item, target)
  }

  function cancel(id: number) {
    const entry = pending.get(id)
    if (!entry || entry.phase !== 'waiting') return
    clearTimeout(entry.timer)
    pending.delete(id)
    toast(`${quoted(entry.item.title)} tekrar Onayda durumunda`)
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
      const updated = await contentsApi.changeStatus(id, entry.target, { keepalive })
      // Fresh counts (the sidebar badge) and fresh views arrive together; only then does the item stop
      // counting as pending (finally), so nothing flips back to the old state for a moment.
      await Promise.all([refreshNavCounts(), alive ? options.onCommitted?.(updated) : undefined])
    } catch (e) {
      const { title, description } = statusChangeError(e)
      const hint = options.failureHint ? ` ${options.failureHint}` : ''
      toast.error(title, { description: `${quoted(entry.item.title)}: ${description}${hint}` })
      if (alive) options.onFailed?.(entry.item)
    } finally {
      pending.delete(id)
    }
  }

  /** Sends every move that is still waiting, right now. */
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

  /** Items moved (waiting or sending), with where they are going. */
  const pendingMoves = computed(() => [...pending.values()].map(({ item, target }) => ({ item, target })))

  return {
    move,
    /** The same undo as the toast's "Geri al": only while the PATCH has not been sent. */
    cancel,
    flush,
    isPending: (id: number) => pending.has(id),
    /** Where a pending move is going, or undefined. */
    targetOf: (id: number): ContentStatus | undefined => pending.get(id)?.target,
    /** True while "Geri al" still works (waiting, not yet sending). */
    canCancel: (id: number) => pending.get(id)?.phase === 'waiting',
    pendingMoves,
  }
}
