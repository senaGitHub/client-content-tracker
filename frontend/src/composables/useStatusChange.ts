import { reactive } from 'vue'
import { toast } from 'vue-sonner'
import { ApiError, contentsApi } from '@/api'
import type { ContentResponse, ContentStatus } from '@/api'
import { STATUS_LABELS } from '@/lib/content'

/**
 * One place for every status change (board drag & drop, card menu, list menu):
 * show the move immediately, confirm it with the server, then offer Undo, or put the item
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
      // Undo is a move back, so it is only offered when the rule allows it (never after publishing).
      const canUndo = !isUndo && updated.allowedTransitions.includes(original.status)
      toast.success(
        isUndo ? `Moved back to ${STATUS_LABELS[target]}` : `Moved to ${STATUS_LABELS[target]}`,
        {
          description: updated.title,
          action: canUndo ? { label: 'Undo', onClick: () => move(updated, original.status, { isUndo: true }) } : undefined,
        },
      )
    } catch (e) {
      place(original)
      if (e instanceof ApiError && e.status === 409) {
        toast.error('Status change not allowed', { description: e.message })
      } else {
        toast.error('Could not change the status', {
          description: e instanceof ApiError ? e.message : 'Please try again.',
        })
      }
    } finally {
      pending.delete(item.id)
    }
  }

  return { move, isPending: (id: number) => pending.has(id) }
}
