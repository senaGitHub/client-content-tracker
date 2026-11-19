<script setup lang="ts">
import type { ContentResponse, ContentStatus } from '@/api'
import { DropdownMenuItem, DropdownMenuLabel } from '@/components/ui/dropdown-menu'
import { STATUS_LABELS, STATUS_STYLES, TRANSITION_LABELS } from '@/lib/content'
import { cn } from '@/lib/utils'

// The "Durumu değiştir" part of a menu: only the moves the rule allows, publishing highlighted.
defineProps<{ item: ContentResponse }>()
const emit = defineEmits<{ move: [status: ContentStatus] }>()
</script>

<template>
  <DropdownMenuLabel class="px-2.5 py-1.5 text-xs font-normal text-muted-foreground">
    {{ item.allowedTransitions.length ? 'Durumu değiştir' : `${STATUS_LABELS[item.status]} son durumdur` }}
  </DropdownMenuLabel>
  <DropdownMenuItem
    v-for="status in item.allowedTransitions"
    :key="status"
    :class="cn(
      'py-[9px]',
      status === 'PUBLISHED' && 'bg-approve-soft font-semibold text-approve-soft-foreground focus:bg-approve-soft focus:text-approve-soft-foreground focus:brightness-95 dark:focus:brightness-125',
    )"
    @select="emit('move', status)"
  >
    <span :class="cn('size-2 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
    {{ TRANSITION_LABELS[status] }}
  </DropdownMenuItem>
  <p v-if="item.allowedTransitions.length" class="px-2.5 pt-1.5 pb-1 text-[11px] text-muted-foreground">
    Yalnızca izin verilen geçişler gösterilir
  </p>
</template>
