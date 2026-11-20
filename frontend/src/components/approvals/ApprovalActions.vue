<script setup lang="ts">
import { CheckIcon, LoaderCircleIcon } from '@lucide/vue'
import { computed, nextTick, ref, watch } from 'vue'
import type { ContentResponse } from '@/api'
import { Button } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import type { ApprovalActions } from '@/composables/useApprovalActions'
import { cn } from '@/lib/utils'

/**
 * Onayla / Revizyon for one approvals row, shared by the Overview panel and the Onay Süreci page.
 * Both are deferred: while the move waits, the pressed button shows a spinner and the other one is
 * hidden (it keeps its place, so the row does not shift). "Geri al" is in the toast. Needs a
 * TooltipProvider above it.
 */
const props = defineProps<{
  item: ContentResponse
  actions: ApprovalActions
  /**
   * Button layout: true = side by side (the Onay Süreci page); false = stacked; 'when-narrow' =
   * side by side, full width, while the nearest @container is under 340px, otherwise stacked (the
   * Overview panel, whose buttons move under the title when it is narrow).
   */
  horizontal?: boolean | 'when-narrow'
  /**
   * A move for this item is in flight elsewhere on the page (the Overview feed's status menu): both
   * buttons are disabled, so no second PATCH can be sent for it.
   */
  locked?: boolean
}>()

const root = ref<HTMLElement>()

const target = computed(() => props.actions.pendingTarget(props.item.id))
const announcement = computed(() => {
  if (target.value === 'PUBLISHED') return `${props.item.title} yayına alınıyor`
  if (target.value === 'DRAFT') return `${props.item.title} taslağa döndürülüyor`
  return ''
})

// The pressed button stays focusable (aria-disabled, not disabled) so keyboard focus is not lost;
// a second press is ignored.
function approve() {
  if (!target.value && !props.locked) props.actions.approve(props.item)
}
function revise() {
  if (!target.value && !props.locked) props.actions.revise(props.item)
}

// After "Geri al" in the toast, focus comes back to the row's first button.
watch(
  () => props.actions.cancelled.value,
  (entry) => {
    if (entry?.id === props.item.id) void nextTick(() => root.value?.querySelector<HTMLElement>('[data-approve]')?.focus())
  },
)

const button = 'h-7 rounded-md px-2.5 text-xs'

// Literal class strings (Tailwind only sees whole class names in source).
const direction = computed(() => {
  if (props.horizontal === 'when-narrow') return 'flex-row @min-[340px]:flex-col'
  return props.horizontal ? 'flex-row' : 'flex-col'
})
/** Makes each button share the row equally whenever the buttons are side by side. */
const grow = computed(() => {
  if (props.horizontal === 'when-narrow') return 'flex-1 @min-[340px]:flex-none'
  return props.horizontal ? 'flex-1' : ''
})
</script>

<template>
  <div ref="root" :class="cn('flex shrink-0 gap-1.5', direction)">
    <Tooltip>
      <TooltipTrigger as-child>
        <Button
          variant="approve"
          size="xs"
          :class="cn(button, grow, target === 'DRAFT' && 'invisible')"
          :aria-label="`Onayla: ${item.title}`"
          :aria-disabled="!!target || undefined"
          :disabled="locked && !target"
          :inert="target === 'DRAFT'"
          data-approve
          @click="approve"
        >
          <LoaderCircleIcon v-if="target === 'PUBLISHED'" class="animate-spin" aria-hidden="true" />
          <CheckIcon v-else :stroke-width="2.8" />
          Onayla
        </Button>
      </TooltipTrigger>
      <TooltipContent side="top" align="end">Onayla ve yayına al · geri alınabilir</TooltipContent>
    </Tooltip>
    <Button
      variant="outline"
      size="xs"
      :class="cn(button, 'text-secondary-text', grow, target === 'PUBLISHED' && 'invisible')"
      :aria-label="`Revizyona gönder: ${item.title}`"
      :aria-disabled="!!target || undefined"
      :disabled="locked && !target"
      :inert="target === 'PUBLISHED'"
      @click="revise"
    >
      <LoaderCircleIcon v-if="target === 'DRAFT'" class="animate-spin" aria-hidden="true" />
      Revizyon
    </Button>

    <!-- Announces the change for screen readers; empty (silent) otherwise. -->
    <span class="sr-only" aria-live="polite">{{ announcement }}</span>
  </div>
</template>
