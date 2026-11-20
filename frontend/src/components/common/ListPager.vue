<script setup lang="ts">
import { computed } from 'vue'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import { cn } from '@/lib/utils'

const props = defineProps<{
  /** One-based. */
  page: number
  pageSize: number
  total: number
  /** Items on the current page (the last page may hold fewer than pageSize). */
  shown: number
}>()
const emit = defineEmits<{ 'update:page': [page: number] }>()

/** "1–20 / 36" */
const range = computed(() => {
  if (props.total === 0) return ''
  const from = (props.page - 1) * props.pageSize + 1
  return `${from}–${from + props.shown - 1} / ${props.total}`
})
const totalPages = computed(() => Math.ceil(props.total / props.pageSize))

const pagerButton = 'h-10 min-w-10 rounded-lg border-input bg-card px-4 text-sm font-semibold text-foreground hover-enabled:bg-muted'
</script>

<template>
  <!-- The range text always shows; the pager only when there is more than one page. -->
  <div v-if="total > 0" class="flex flex-col items-center gap-3 px-2.5 pt-3.5 pb-1.5 sm:flex-row sm:justify-between">
    <p class="text-[13px] text-muted-foreground tabular-nums" aria-live="polite">{{ range }}<span class="sr-only"> içerik</span></p>
    <Pagination
      v-if="totalPages > 1"
      :page="page"
      :total="total"
      :items-per-page="pageSize"
      :sibling-count="1"
      show-edges
      class="mx-0 w-auto"
      @update:page="(p: number) => emit('update:page', p)"
    >
      <PaginationContent v-slot="{ items }" class="gap-1.5">
        <PaginationPrevious :class="cn(pagerButton, 'pl-4!')">Önceki</PaginationPrevious>
        <template v-for="(entry, index) in items" :key="index">
          <PaginationItem
            v-if="entry.type === 'page'"
            :value="entry.value"
            :is-active="entry.value === page"
            :class="cn(pagerButton, 'px-3', entry.value === page && 'border-nav-active bg-coral-soft text-coral-text hover-enabled:bg-coral-soft')"
          >
            {{ entry.value }}
          </PaginationItem>
          <PaginationEllipsis v-else :index="index" />
        </template>
        <PaginationNext :class="cn(pagerButton, 'pr-4!')">Sonraki</PaginationNext>
      </PaginationContent>
    </Pagination>
  </div>
</template>
