<script setup lang="ts">
import { SearchIcon, XIcon } from '@lucide/vue'
import { useDebounceFn } from '@vueuse/core'
import { computed, ref, watch } from 'vue'
import type { ContentType } from '@/api'
import FilterSelect from '@/components/content/FilterSelect.vue'
import { useClientOptions } from '@/composables/useClientOptions'
import type { ContentQuery, DueFilter } from '@/composables/useContentQuery'
import { DUE_OPTIONS, TYPE_LABELS, TYPE_ORDER } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = defineProps<{
  query: ContentQuery
  hasFilters: boolean
  /**
   * Search on the left, filters on the right (the board, which has no status tabs). The filters
   * still wrap under the search when there is no room (justify-between keeps a lone item left).
   */
  spread?: boolean
}>()
const emit = defineEmits<{ update: [patch: Partial<ContentQuery>]; clear: [] }>()

const { clients } = useClientOptions()

const clientOptions = computed(() => clients.value.map((c) => ({ value: String(c.id), label: c.name })))
const TYPE_OPTIONS = TYPE_ORDER.map((t) => ({ value: t, label: TYPE_LABELS[t] }))

// Typing updates the box at once; the URL (and the request) follow after a short pause.
const search = ref(props.query.q)
const commitSearch = useDebounceFn((value: string) => emit('update', { q: value }), 300)
watch(search, (value) => {
  if (value !== props.query.q) void commitSearch(value)
})
// Keep the box in sync when the URL changes from outside (Back button, Filtreleri temizle).
watch(() => props.query.q, (value) => {
  if (value !== search.value) search.value = value
})

function clearSearch() {
  search.value = ''
  emit('update', { q: '' })
}
</script>

<template>
  <div role="search" aria-label="İçerikleri filtrele" :class="cn('flex flex-wrap items-center gap-3', spread && 'justify-between')">
    <label
      class="flex h-8 w-full items-center gap-2 rounded-lg border border-input bg-card px-3 transition-shadow focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 sm:w-[220px]"
    >
      <SearchIcon class="size-4 shrink-0 text-foreground" aria-hidden="true" />
      <span class="sr-only">İçerik ara</span>
      <input
        v-model="search"
        type="search"
        placeholder="İçerik ara..."
        class="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground [&::-webkit-search-cancel-button]:hidden"
        @keydown.esc="clearSearch"
      />
      <button
        v-if="search"
        type="button"
        class="-mr-1.5 flex size-6 shrink-0 items-center justify-center rounded-md text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
        aria-label="Aramayı temizle"
        @click="clearSearch"
      >
        <XIcon class="size-3.5" />
      </button>
    </label>

    <!-- Filters and "Filtreleri temizle" move as one group, so they wrap together. -->
    <div class="flex w-full flex-wrap items-center gap-3 sm:w-auto">
      <div class="grid w-full grid-cols-2 gap-3 sm:flex sm:w-auto sm:flex-wrap">
        <FilterSelect
          :model-value="query.clientId ? String(query.clientId) : undefined"
          :options="clientOptions"
          all-label="Tüm Markalar"
          label="Marka"
          size="sm"
          class="sm:max-w-56"
          @update:model-value="(v) => emit('update', { clientId: v ? Number(v) : undefined })"
        />
        <FilterSelect
          :model-value="query.type"
          :options="TYPE_OPTIONS"
          all-label="Tüm Türler"
          label="Tür"
          size="sm"
          @update:model-value="(v) => emit('update', { type: v as ContentType | undefined })"
        />
        <FilterSelect
          :model-value="query.due === 'all' ? undefined : query.due"
          :options="DUE_OPTIONS"
          all-label="Teslim: Tümü"
          prefix="Teslim: "
          label="Teslim"
          size="sm"
          :tone="query.due === 'overdue' ? 'overdue' : 'coral'"
          class="col-span-2 sm:col-span-1"
          @update:model-value="(v) => emit('update', { due: (v as DueFilter | undefined) ?? 'all' })"
        />
      </div>

      <button
        v-if="hasFilters"
        type="button"
        class="rounded-md px-1 text-sm font-semibold text-coral-text outline-none hover:underline hover:underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50"
        @click="emit('clear')"
      >
        Filtreleri temizle
      </button>

    </div>
  </div>
</template>
