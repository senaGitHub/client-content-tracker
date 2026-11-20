<script setup lang="ts">
import type { ApprovalItem } from '@/api'
import ApprovalActions from '@/components/approvals/ApprovalActions.vue'
import { QUEUE_COLUMNS } from '@/components/approvals/columns'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import { useContentDialog } from '@/composables/useContentDialog'
import type { ApprovalActions as Actions } from '@/composables/useApprovalActions'
import { vHighlight } from '@/composables/useHighlight'
import { dueHint, formatDate, waitingLabel } from '@/lib/format'
import { cn } from '@/lib/utils'

/**
 * One row of the Onay Süreci page. Desktop: one line with İçerik / Marka / Bekleme / Teslim columns
 * and the actions on the right. Mobile: title, then a wrapping meta line, then full-width buttons.
 */
defineProps<{ item: ApprovalItem; actions: Actions }>()

const { openEdit } = useContentDialog()
</script>

<template>
  <!-- role="row" / role="cell": one row of the page's ARIA table (see ApprovalsView). -->
  <li
    v-highlight:content="item.id"
    role="row"
    :class="cn(
      'flex cursor-pointer flex-col gap-2.5 border-t border-divider px-2.5 py-3 transition-colors first:border-t-0 hover:bg-coral-soft/40',
      QUEUE_COLUMNS,
    )"
    @click="openEdit(item)"
  >
    <!-- İçerik: type icon (with its tooltip and screen-reader label), title, channel. -->
    <div role="cell" class="flex min-w-0 items-start gap-2.5">
      <ContentTypeIcon :type="item.type" class="mt-0.5 shrink-0" />
      <div class="min-w-0">
        <!-- A real button so keyboard users can open the sheet; the whole row is clickable too.
             On desktop a long title wraps in full instead of being clamped. -->
        <button
          type="button"
          class="line-clamp-2 max-w-full rounded-sm text-left text-sm leading-snug font-semibold break-words outline-none focus-visible:ring-3 focus-visible:ring-ring/50 md:line-clamp-none"
          :title="item.title"
          @click.stop="openEdit(item)"
        >
          {{ item.title }}
        </button>
        <p v-if="item.channel" class="mt-0.5 truncate text-xs text-muted-foreground">{{ item.channel }}</p>
      </div>
    </div>

    <!-- Marka, bekleme, teslim: columns on desktop, one wrapping line under the title on mobile. -->
    <div role="none" class="flex flex-wrap items-center gap-x-3 gap-y-1.5 pl-[26px] text-sm md:contents">
      <span role="cell" class="min-w-0 leading-snug break-words">{{ item.client.name }}</span>
      <span role="cell" class="min-w-0">
        <span
          v-if="waitingLabel(item.inReviewSince)"
          class="inline-block rounded-sm bg-muted px-2 py-0.5 text-xs font-semibold whitespace-nowrap text-secondary-text"
        >
          {{ waitingLabel(item.inReviewSince) }}
        </span>
      </span>
      <span role="cell" class="min-w-0 whitespace-nowrap">
        <span class="sr-only">Teslim: </span>
        <span :class="item.overdue ? 'font-semibold text-overdue-text' : ''">{{ formatDate(item.dueDate) }}</span>
        <span
          v-if="dueHint(item)"
          :class="cn('ml-1.5 text-xs md:ml-0 md:block', item.overdue ? 'text-overdue-text' : 'text-muted-foreground')"
        >
          {{ dueHint(item) }}
        </span>
      </span>
    </div>

    <!-- A click on the buttons must not open the sheet. -->
    <div role="cell" @click.stop>
      <ApprovalActions :item="item" :actions="actions" horizontal />
    </div>
  </li>
</template>
