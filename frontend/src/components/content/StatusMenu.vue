<script setup lang="ts">
import { ChevronDownIcon, LoaderCircleIcon } from '@lucide/vue'
import type { ContentResponse, ContentStatus } from '@/api'
import StatusBadge from '@/components/content/StatusBadge.vue'
import StatusMoveItems from '@/components/content/StatusMoveItems.vue'
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { STATUS_LABELS } from '@/lib/content'

// Inline status control: the badge opens a menu with the moves the backend allows for this item.
defineProps<{ item: ContentResponse; pending?: boolean }>()
const emit = defineEmits<{ move: [status: ContentStatus] }>()
// Every badge here is `uniform` (as wide as the longest label), so the chevrons line up.
</script>

<template>
  <span v-if="pending" class="inline-flex items-center gap-1.5" aria-live="polite">
    <StatusBadge :status="item.status" uniform />
    <LoaderCircleIcon class="size-3.5 animate-spin text-muted-foreground" aria-hidden="true" />
    <span class="sr-only">Durum kaydediliyor</span>
  </span>

  <StatusBadge
    v-else-if="item.allowedTransitions.length === 0"
    :status="item.status"
    uniform
    :title="`${STATUS_LABELS[item.status]} son durumdur, değiştirilemez`"
  />

  <DropdownMenu v-else>
    <DropdownMenuTrigger
      :aria-label="`Durum: ${STATUS_LABELS[item.status]}. Durumu değiştir`"
      class="group inline-flex items-center gap-1 rounded-full outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <StatusBadge :status="item.status" uniform class="transition-[filter] group-hover:brightness-95 dark:group-hover:brightness-110" />
      <ChevronDownIcon class="size-3.5 text-foreground" aria-hidden="true" />
    </DropdownMenuTrigger>
    <DropdownMenuContent align="start" class="w-[200px] min-w-[200px]">
      <StatusMoveItems :item="item" @move="(status) => emit('move', status)" />
    </DropdownMenuContent>
  </DropdownMenu>
</template>
