<script setup lang="ts">
import type { ContentResponse, ContentStatus } from '@/api'
import ContentActionsMenu from '@/components/content/ContentActionsMenu.vue'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import OverdueChip from '@/components/content/OverdueChip.vue'
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
      'board-card relative flex flex-col gap-2.5 rounded-2xl border bg-card p-3 transition-[border-color,box-shadow] hover:border-input hover:shadow-card',
      pending ? 'is-pending cursor-progress opacity-70' : 'cursor-grab active:cursor-grabbing',
    )"
  >
    <div class="flex gap-2.5">
      <img
        v-if="item.client.coverImageUrl"
        :src="item.client.coverImageUrl"
        alt=""
        loading="lazy"
        draggable="false"
        class="h-[46px] w-[62px] shrink-0 rounded-[9px] object-cover"
      />
      <span v-else class="h-[46px] w-[62px] shrink-0 rounded-[9px] bg-muted" aria-hidden="true" />

      <div class="min-w-0 flex-1">
        <!-- The title button stretches over the card, so the whole card opens the edit sheet. -->
        <h3 class="text-sm leading-[1.3] font-bold break-words text-foreground">
          <button
            type="button"
            class="text-left outline-none after:absolute after:inset-0 after:rounded-2xl focus-visible:after:ring-3 focus-visible:after:ring-ring/50"
            @click="emit('open')"
          >
            {{ item.title }}
          </button>
        </h3>
        <p class="mt-0.5 truncate text-[13px] text-muted-foreground">{{ item.client.name }}</p>
      </div>

      <ContentActionsMenu :item="item" :pending="pending" class="-mt-1 -mr-1" @open="emit('open')" @move="(s) => emit('move', s)" />
    </div>

    <div class="flex flex-wrap items-center gap-x-2 gap-y-1.5">
      <ContentTypeIcon :type="item.type" with-label class="min-w-0" />
      <span class="ml-auto shrink-0">
        <OverdueChip v-if="item.overdue" :due-date="item.dueDate" />
        <span v-else class="text-xs text-muted-foreground">{{ dueLabel(item) }}</span>
      </span>
    </div>
  </article>
</template>
