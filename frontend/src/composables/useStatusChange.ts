import { reactive } from 'vue'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import type { ContentResponse, ContentStatus } from '@/api'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { STATUS_LABELS } from '@/lib/content'
import { statusChangeError } from '@/lib/errors'

/** Toast text for a move, e.g. "Yaz kampanyası onaya gönderildi". */
function movedMessage(title: string, from: ContentStatus, to: ContentStatus): string {
  if (to === 'IN_REVIEW') return `${title} onaya gönderildi`
  if (to === 'PUBLISHED') return `${title} yayınlandı`
  if (from === 'IN_REVIEW' && to === 'DRAFT') return `${title} revizyona gönderildi`
  return `${title} ${STATUS_LABELS[to]} durumuna taşındı`
}

/**
 * One place for every status change (board drag & drop, card menu, list menu):
 * show the move immediately, confirm it with the server, then offer "Geri al", or put the item
 * back and explain why if the server refuses (409) or can't be reached.
 *
 * `place(item)` is the view's way to show an item in its current state (e.g. move a card to
 * the column of `item.status`); it is called for the optimistic move, the server's answer
 * and the rollback alike.
 */
export function useStatusChange(place: (item: ContentResponse) => void) {
  const pending = reactive(new Set<number>())

  async function move(item: ContentResponse, target: ContentStatus, { isUndo = false } = {}) {
    if (pending.has(item.id) || item.status === target) return
    const original = item
    pending.add(item.id)
    // Until the server answers, the item has no known next steps.
    place({ ...item, status: target, allowedTransitions: [] })
    try {
      const updated = await contentsApi.changeStatus(item.id, target)
      place(updated)
      void refreshNavCounts()
      // Undo is a move back, so it is only offered when the rule allows it (never after publishing).
      const canUndo = !isUndo && updated.allowedTransitions.includes(original.status)
      toast.success(
        isUndo ? `${updated.title} tekrar ${STATUS_LABELS[target]} durumunda` : movedMessage(updated.title, original.status, target),
        {
          action: canUndo
            ? { label: 'Geri al', onClick: () => move(updated, original.status, { isUndo: true }) }
            : undefined,
        },
      )
    } catch (e) {
      place(original)
      const { title, description } = statusChangeError(e)
      // On the board the card snaps back, so say so (matches the 409 toast in the mockups).
      toast.error(title, { description: `${description} İçerik eski yerine döndü.` })
    } finally {
      pending.delete(item.id)
    }
  }

  return { move, isPending: (id: number) => pending.has(id) }
}
