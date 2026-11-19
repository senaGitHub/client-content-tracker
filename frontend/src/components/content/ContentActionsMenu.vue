<script setup lang="ts">
import { EllipsisIcon, LoaderCircleIcon, PencilIcon } from '@lucide/vue'
import type { ContentResponse, ContentStatus } from '@/api'
import StatusMoveItems from '@/components/content/StatusMoveItems.vue'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

// The "⋯" menu on board cards and list rows: edit, plus the allowed status moves.
defineProps<{ item: ContentResponse; pending?: boolean }>()
const emit = defineEmits<{ open: []; move: [status: ContentStatus] }>()
</script>

<template>
  <span v-if="pending" class="flex size-8 shrink-0 items-center justify-center" aria-hidden="true">
    <LoaderCircleIcon class="size-4 animate-spin text-muted-foreground" />
  </span>
  <DropdownMenu v-else>
    <DropdownMenuTrigger
      class="relative z-10 flex size-8 shrink-0 items-center justify-center rounded-lg text-secondary-text outline-none hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 data-[state=open]:bg-muted"
      :aria-label="`İşlemler: ${item.title}`"
      @click.stop
    >
      <EllipsisIcon class="size-5" :stroke-width="2.4" />
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-[200px] min-w-[200px]">
      <DropdownMenuItem @select="emit('open')">
        <PencilIcon />
        Düzenle
      </DropdownMenuItem>
      <DropdownMenuSeparator />
      <StatusMoveItems :item="item" @move="(status) => emit('move', status)" />
    </DropdownMenuContent>
  </DropdownMenu>
</template>
