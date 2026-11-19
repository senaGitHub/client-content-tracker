<script setup lang="ts">
import { EllipsisIcon, LoaderCircleIcon, PencilIcon } from '@lucide/vue'
import type { ContentResponse, ContentStatus } from '@/api'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { STATUS_LABELS, STATUS_STYLES } from '@/lib/content'
import { dueLabel } from '@/lib/format'
import { cn } from '@/lib/utils'

const props = defineProps<{ item: ContentResponse; pending?: boolean }>()
const emit = defineEmits<{ open: []; move: [status: ContentStatus] }>()
</script>

<template>
  <article
    :data-id="props.item.id"
    :aria-busy="pending"
    :class="cn(
      'group relative rounded-xl border bg-card p-3 transition-colors hover:border-foreground/15',
      pending ? 'is-pending cursor-progress opacity-70' : 'cursor-grab active:cursor-grabbing',
    )"
  >
    <div class="flex items-start gap-2">
      <!-- The title button stretches over the card, so the whole card opens the editor. -->
      <h3 class="min-w-0 flex-1 text-sm leading-snug font-medium text-foreground">
        <button
          type="button"
          class="rounded-sm text-left outline-none after:absolute after:inset-0 after:rounded-xl focus-visible:after:ring-3 focus-visible:after:ring-ring/50"
          @click="emit('open')"
        >
          {{ item.title }}
        </button>
      </h3>

      <LoaderCircleIcon v-if="pending" class="relative mt-0.5 size-4 shrink-0 animate-spin text-muted-foreground" aria-hidden="true" />
      <DropdownMenu v-else>
        <DropdownMenuTrigger
          class="relative z-10 -mt-1 -mr-1 flex size-7 shrink-0 items-center justify-center rounded-md text-muted-foreground outline-none hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 data-[state=open]:bg-muted"
          :aria-label="`Actions for ${item.title}`"
        >
          <EllipsisIcon class="size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="min-w-48">
          <DropdownMenuItem @select="emit('open')">
            <PencilIcon />
            Edit
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuLabel class="text-xs font-normal text-muted-foreground">
            {{ item.allowedTransitions.length ? 'Move to' : `${STATUS_LABELS[item.status]} is final` }}
          </DropdownMenuLabel>
          <DropdownMenuItem v-for="status in item.allowedTransitions" :key="status" @select="emit('move', status)">
            <span :class="cn('size-2 rounded-full', STATUS_STYLES[status].dot)" aria-hidden="true" />
            {{ STATUS_LABELS[status] }}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>

    <p class="mt-1 truncate text-xs text-muted-foreground">{{ item.client.name }}</p>

    <div class="mt-3 flex items-center justify-between gap-2">
      <span class="flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
        <ContentTypeIcon :type="item.type" size="sm" />
        <span v-if="item.channel" class="truncate">{{ item.channel }}</span>
      </span>
      <span :class="cn('shrink-0 text-xs', item.overdue ? 'font-medium text-overdue' : 'text-muted-foreground')">
        {{ dueLabel(item) }}
      </span>
    </div>
  </article>
</template>
