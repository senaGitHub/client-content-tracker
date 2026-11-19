<script setup lang="ts">
import { computed } from 'vue'
import type { ContentResponse } from '@/api'
import ContentTypeIcon from '@/components/content/ContentTypeIcon.vue'
import StatusBadge from '@/components/content/StatusBadge.vue'
import { clientLink } from '@/lib/content'
import { dueLabel } from '@/lib/format'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{ item: ContentResponse; showStatus?: boolean }>(), {
  showStatus: true,
})

const due = computed(() => dueLabel(props.item))
</script>

<template>
  <!-- Phase 6: rows open the client; Phase 7 switches this to the Edit content dialog. -->
  <RouterLink
    :to="clientLink(item.client.id)"
    class="flex items-center gap-3 rounded-lg px-2 py-2 outline-none transition-colors hover:bg-muted/60 focus-visible:ring-3 focus-visible:ring-ring/50"
  >
    <ContentTypeIcon :type="item.type" />
    <span class="min-w-0 flex-1">
      <span class="block truncate text-sm font-medium text-foreground">{{ item.title }}</span>
      <span class="mt-0.5 flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
        <span class="truncate">{{ item.client.name }}</span>
        <span aria-hidden="true">·</span>
        <span :class="cn('shrink-0', item.overdue && 'font-medium text-overdue')">{{ due }}</span>
      </span>
    </span>
    <StatusBadge v-if="showStatus" :status="item.status" size="sm" />
  </RouterLink>
</template>
