<script setup lang="ts">
import { ChevronDownIcon, LoaderCircleIcon } from '@lucide/vue'
import type { ContentResponse, ContentStatus } from '@/api'
import StatusBadge from '@/components/content/StatusBadge.vue'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { STATUS_LABELS, STATUS_STYLES } from '@/lib/content'
import { cn } from '@/lib/utils'

// Inline status control: the badge opens a menu with the moves the backend allows for this item.
defineProps<{ item: ContentResponse; pending?: boolean }>()
const emit = defineEmits<{ move: [status: ContentStatus] }>()
</script>

<template>
  <span v-if="pending" class="inline-flex items-center gap-1.5" aria-live="polite">
    <StatusBadge :status="item.status" />
    <LoaderCircleIcon class="size-3.5 animate-spin text-muted-foreground" aria-hidden="true" />
    <span class="sr-only">Saving status</span>
  </span>

  <StatusBadge
    v-else-if="item.allowedTransitions.length === 0"
    :status="item.status"
    :title="`${STATUS_LABELS[item.status]} is final`"
  />

  <DropdownMenu v-else>
    <DropdownMenuTrigger
      :aria-label="`Status: ${STATUS_LABELS[item.status]}. Change status`"
      class="group inline-flex items-center gap-0.5 rounded-full outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <StatusBadge :status="item.status" class="transition-[filter] group-hover:brightness-95 dark:group-hover:brightness-110" />
      <ChevronDownIcon class="size-3.5 text-muted-foreground" aria-hidden="true" />
    </DropdownMenuTrigger>
    <DropdownMenuContent align="start" class="min-w-44">
      <DropdownMenuLabel class="text-xs font-normal text-muted-foreground">Move to</DropdownMenuLabel>
      <DropdownMenuItem v-for="status in item.allowedTransitions" :key="status" @select="emit('move', status)">
        <span :class="cn('size-2 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
        {{ STATUS_LABELS[status] }}
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
