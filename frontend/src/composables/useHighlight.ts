import type { Directive } from 'vue'
import { nextTick } from 'vue'

/**
 * "Show me what I just created", without changing any sort order.
 *
 * After a create, the dialog calls `highlight('content', id)` (or 'client'). Rows and cards carry
 * `v-highlight:content="item.id"` / `v-highlight:client="client.id"`. The page reloads its list on
 * its own (onSaved); when the new item's element is on screen in that list, it is scrolled into
 * view and flashes coral-soft for about 2 seconds, once. If the item is not on the current page
 * (another tab, filter or page of results), nothing happens: the toast's "Görüntüle" still finds it.
 */

export type HighlightKind = 'content' | 'client'

const ATTR = 'data-highlight'
const FLASH_CLASS = 'highlight-flash'
/** Matches the keyframes in main.css. */
const FLASH_MS = 2000
/** How long a request waits for its element (the list reload) before it is dropped. */
const WAIT_MS = 8000

let target: { key: string; until: number } | null = null

const keyOf = (kind: string | undefined, id: number) => `${kind}-${id}`

/** Flashes `el` if it is the pending target and actually rendered (not a hidden mobile/desktop twin). */
function tryFlash(el: HTMLElement): boolean {
  if (!target || el.getAttribute(ATTR) !== target.key) return false
  if (Date.now() > target.until) {
    target = null
    return false
  }
  if (el.getClientRects().length === 0) return false
  target = null

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  el.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: reduceMotion ? 'auto' : 'smooth' })
  el.classList.remove(FLASH_CLASS)
  void el.offsetWidth // restart the animation if the class was still on
  el.classList.add(FLASH_CLASS)
  window.setTimeout(() => el.classList.remove(FLASH_CLASS), FLASH_MS)
  return true
}

/** Asks the current page to highlight this item once it shows it. */
export function highlight(kind: HighlightKind, id: number) {
  target = { key: keyOf(kind, id), until: Date.now() + WAIT_MS }
  // The element may already be on screen (e.g. kept by a keyed list); otherwise the directive
  // catches it when the reloaded list mounts or updates it.
  void nextTick(() => {
    for (const el of document.querySelectorAll<HTMLElement>(`[${ATTR}]`)) {
      if (tryFlash(el)) break
    }
  })
}

/** `v-highlight:content="item.id"`: marks a row or card as a highlight target. */
export const vHighlight: Directive<HTMLElement, number> = {
  mounted(el, binding) {
    el.setAttribute(ATTR, keyOf(binding.arg, binding.value))
    tryFlash(el)
  },
  updated(el, binding) {
    el.setAttribute(ATTR, keyOf(binding.arg, binding.value))
    tryFlash(el)
  },
}
