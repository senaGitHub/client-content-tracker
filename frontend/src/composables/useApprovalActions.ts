import { computed, ref, toValue } from 'vue'
import type { MaybeRefOrGetter } from 'vue'
import type { ApprovalItem, ContentResponse } from '@/api'
import { useDeferredMove } from '@/composables/useDeferredMove'
import { quoted } from '@/lib/format'

/**
 * Onayla / Revizyon for an approvals queue: the Overview panel and the Onay Süreci page.
 *
 * Both are deferred the same way (useDeferredMove): the row stays in place, the pressed button
 * shows a spinner and the other one is hidden. The toast (bottom right) offers "Geri al", which
 * cancels the move so nothing is sent. Otherwise the PATCH (→ PUBLISHED or → DRAFT) goes after a
 * few seconds, and leaving the page or closing the tab sends it at once. Nothing else changes while
 * it waits: once the PATCH is done, `refresh` (awaited) reloads the view, so the row leaves together
 * with the new numbers and the sidebar badge.
 *
 * @param items   the queue as loaded from the server (undefined while loading)
 * @param refresh reloads everything on the page that shows these items
 */
export function useApprovalActions(
  items: MaybeRefOrGetter<ApprovalItem[] | undefined>,
  refresh: () => Promise<unknown>,
) {
  /** The last move cancelled with "Geri al" (`n` changes every time), so the row can take focus back. */
  const cancelled = ref<{ id: number; n: number }>()

  const deferred = useDeferredMove({
    onCommitted: refresh,
    onFailed: () => void refresh(),
    onCancel: (item) => {
      cancelled.value = { id: item.id, n: (cancelled.value?.n ?? 0) + 1 }
    },
    toastText: (item, target) =>
      target === 'DRAFT'
        ? { title: `${quoted(item.title)} taslağa döndürülüyor`, description: 'Birkaç saniye içinde taslağa dönecek.' }
        : { title: `${quoted(item.title)} yayına alınıyor`, description: 'Birkaç saniye içinde yayınlanacak.' },
  })

  return {
    /** The queue; rows stay until fresh data replaces it. */
    visible: computed(() => toValue(items)),
    approve: (item: ContentResponse) => deferred.move(item, 'PUBLISHED'),
    revise: (item: ContentResponse) => deferred.move(item, 'DRAFT'),
    /** Where this item's waiting (or sending) move goes: 'PUBLISHED', 'DRAFT', or undefined. */
    pendingTarget: deferred.targetOf,
    cancelled,
    /** Every item with a waiting (or sending) move, any target, so the Overview feed can block it. */
    pendingIds: computed(() => deferred.pendingMoves.value.map((entry) => entry.item.id)),
  }
}

export type ApprovalActions = ReturnType<typeof useApprovalActions>
