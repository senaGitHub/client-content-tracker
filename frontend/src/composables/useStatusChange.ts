import { computed, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import type { ContentResponse, ContentStatus } from '@/api'
import { DEFER_DELAY_MS, useDeferredMove } from '@/composables/useDeferredMove'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { STATUS_LABELS } from '@/lib/content'
import { statusChangeError } from '@/lib/errors'

/**
 * Why `place` is being called:
 * - 'optimistic': shown before the server answered (or, for a publish, before it was even sent);
 * - 'confirmed': the server's answer;
 * - 'reverted': back to how it was (refused, unreachable, or a publish cancelled with "Geri al").
 */
export type PlacePhase = 'optimistic' | 'confirmed' | 'reverted'

const BACK_HINT = 'İçerik eski yerine döndü.'

/** Toast text for a move, e.g. "Yaz kampanyası onaya gönderildi". */
function movedMessage(title: string, from: ContentStatus, to: ContentStatus): string {
  if (to === 'IN_REVIEW') return `${title} onaya gönderildi`
  if (from === 'IN_REVIEW' && to === 'DRAFT') return `${title} revizyona gönderildi`
  return `${title} ${STATUS_LABELS[to]} durumuna taşındı`
}

/** How an item looks while its publish is waiting: already published, with no next moves. */
export function asPublishing(item: ContentResponse): ContentResponse {
  return { ...item, status: 'PUBLISHED', overdue: false, allowedTransitions: [] }
}

/**
 * One place for every status change (board drag & drop, card menu, list menu, feed, edit sheet).
 *
 * One rule, everywhere:
 * - To PUBLISHED (when allowed): cannot be undone on the server, so the PATCH waits ~5 s and
 *   "Geri al" cancels it before anything is sent (useDeferredMove).
 * - Any other move: sent at once; "Geri al" sends the reverse move (when the rule allows it).
 * A refused move (409) or a network error puts the item back and explains why.
 *
 * `place(item, phase)` is the view's way to show an item in its current state (e.g. move a card to
 * the column of `item.status`). It may return a promise (e.g. a reload); for a committed publish it
 * is awaited, so the item counts as pending until the view has fresh data.
 */
export function useStatusChange(place: (item: ContentResponse, phase: PlacePhase) => Promise<unknown> | void) {
  const pending = reactive(new Set<number>())

  const publishing = useDeferredMove({
    onStart: (item) => place(asPublishing(item), 'optimistic'),
    onCancel: (item) => place(item, 'reverted'),
    onCommitted: (updated) => place(updated, 'confirmed'),
    onFailed: (item) => place(item, 'reverted'),
    failureHint: BACK_HINT,
  })

  function isPending(id: number) {
    return pending.has(id) || publishing.isPending(id)
  }

  async function move(item: ContentResponse, target: ContentStatus, { isUndo = false } = {}) {
    if (isPending(item.id) || item.status === target) return
    // Only a publish the rule allows is deferred. A refused one (e.g. a draft dropped on Yayında)
    // goes straight to the server so its 409 snaps the card back at once.
    if (target === 'PUBLISHED' && item.allowedTransitions.includes('PUBLISHED')) {
      publishing.move(item, 'PUBLISHED')
      return
    }

    const original = item
    pending.add(item.id)
    // Until the server answers, the item has no known next steps.
    place({ ...item, status: target, allowedTransitions: [] }, 'optimistic')
    try {
      const updated = await contentsApi.changeStatus(item.id, target)
      place(updated, 'confirmed')
      void refreshNavCounts()
      // Undo is the reverse move, so it is only offered when the rule allows it.
      const canUndo = !isUndo && updated.allowedTransitions.includes(original.status)
      toast.success(
        isUndo ? `${updated.title} tekrar ${STATUS_LABELS[target]} durumunda` : movedMessage(updated.title, original.status, target),
        {
          // Every status toast that offers "Geri al" stays as long as a deferred move waits (hover still pauses it).
          duration: canUndo ? DEFER_DELAY_MS : undefined,
          action: canUndo
            ? { label: 'Geri al', onClick: () => move(updated, original.status, { isUndo: true }) }
            : undefined,
        },
      )
    } catch (e) {
      place(original, 'reverted')
      const { title, description } = statusChangeError(e)
      toast.error(title, { description: `${description} ${BACK_HINT}` })
    } finally {
      pending.delete(item.id)
    }
  }

  return {
    move,
    isPending,
    /** Publishes still waiting for their PATCH; views re-apply them after reloading data. */
    publishingItems: computed(() => publishing.pendingMoves.value.map((entry) => entry.item)),
    /** Sends waiting publishes now (e.g. when the edit sheet's page is left). */
    flush: publishing.flush,
  }
}
