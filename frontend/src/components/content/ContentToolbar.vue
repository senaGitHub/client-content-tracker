<script setup lang="ts">
import { SearchIcon, XIcon } from '@lucide/vue'
import { useDebounceFn } from '@vueuse/core'
import { ref, watch } from 'vue'
import type { ContentStatus, ContentType } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useClientOptions } from '@/composables/useClientOptions'
import type { ContentQuery, DueFilter } from '@/composables/useContentQuery'
import { STATUS_LABELS, STATUS_ORDER, TYPE_LABELS } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = defineProps<{ query: ContentQuery; showStatus: boolean; hasFilters: boolean }>()
const emit = defineEmits<{ update: [patch: Partial<ContentQuery>]; clear: [] }>()

const { clients } = useClientOptions()

// Select items can't have an empty value, so "no filter" is the 'all' sentinel.
const ALL = 'all'
const TYPES = Object.keys(TYPE_LABELS) as ContentType[]
const DUE_OPTIONS: { value: DueFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'overdue', label: 'Overdue' },
  { value: 'week', label: 'Due this week' },
]

// Typing updates the box at once; the URL (and the request) follow after a short pause.
const search = ref(props.query.q)
const commitSearch = useDebounceFn((value: string) => emit('update', { q: value }), 300)
watch(search, (value) => {
  if (value !== props.query.q) commitSearch(value)
})
// Keep the box in sync when the URL changes from outside (Back button, Clear filters).
watch(() => props.query.q, (value) => {
  if (value !== search.value) search.value = value
})

function clearSearch() {
  search.value = ''
  emit('update', { q: '' })
}
</script>

<template>
  <div class="flex flex-col gap-3 lg:flex-row lg:flex-wrap lg:items-center" role="search" aria-label="Filter content">
    <div class="relative w-full lg:w-64">
      <SearchIcon class="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
      <Input
        v-model="search"
        type="search"
        class="h-9 pr-8 pl-8 [&::-webkit-search-cancel-button]:hidden"
        placeholder="Search titles"
        aria-label="Search titles"
        @keydown.esc="clearSearch"
      />
      <button
        v-if="search"
        type="button"
        class="absolute top-1/2 right-1.5 flex size-6 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
        aria-label="Clear search"
        @click="clearSearch"
      >
        <XIcon class="size-3.5" />
      </button>
    </div>

    <div class="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
      <Select
        :model-value="query.clientId ? String(query.clientId) : ALL"
        @update:model-value="(v) => emit('update', { clientId: v === ALL ? undefined : Number(v) })"
      >
        <SelectTrigger class="h-9 w-full sm:w-44" aria-label="Client">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem :value="ALL">All clients</SelectItem>
          <SelectItem v-for="client in clients" :key="client.id" :value="String(client.id)">{{ client.name }}</SelectItem>
        </SelectContent>
      </Select>

      <Select
        :model-value="query.type ?? ALL"
        @update:model-value="(v) => emit('update', { type: v === ALL ? undefined : (v as ContentType) })"
      >
        <SelectTrigger class="h-9 w-full sm:w-36" aria-label="Type">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem :value="ALL">All types</SelectItem>
          <SelectItem v-for="type in TYPES" :key="type" :value="type">{{ TYPE_LABELS[type] }}</SelectItem>
        </SelectContent>
      </Select>

      <Select
        v-if="showStatus"
        :model-value="query.status ?? ALL"
        @update:model-value="(v) => emit('update', { status: v === ALL ? undefined : (v as ContentStatus) })"
      >
        <SelectTrigger class="h-9 w-full sm:w-36" aria-label="Status">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem :value="ALL">All statuses</SelectItem>
          <SelectItem v-for="status in STATUS_ORDER" :key="status" :value="status">{{ STATUS_LABELS[status] }}</SelectItem>
        </SelectContent>
      </Select>
    </div>

    <div role="group" aria-label="Due date" class="inline-flex h-9 w-full items-center rounded-lg bg-muted p-0.5 sm:w-fit">
      <button
        v-for="option in DUE_OPTIONS"
        :key="option.value"
        type="button"
        :aria-pressed="query.due === option.value"
        :class="cn(
          'h-full flex-1 rounded-md px-3 text-sm font-medium whitespace-nowrap outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 sm:flex-none',
          query.due === option.value
            ? 'border border-border bg-card text-foreground'
            : 'text-muted-foreground hover:text-foreground',
          query.due === option.value && option.value === 'overdue' && 'text-overdue-text',
        )"
        @click="emit('update', { due: option.value })"
      >
        {{ option.label }}
      </button>
    </div>

    <Button v-if="hasFilters" variant="ghost" class="h-9 self-start text-muted-foreground lg:self-auto" @click="emit('clear')">
      <XIcon />
      Clear filters
    </Button>
  </div>
</template>
