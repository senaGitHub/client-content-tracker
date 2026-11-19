<script setup lang="ts">
import { ArrowDownIcon, ArrowUpDownIcon, ArrowUpIcon } from '@lucide/vue'
import { computed } from 'vue'

const props = defineProps<{ label: string; property: string; sort: { property: string; direction: 'asc' | 'desc' } }>()
const emit = defineEmits<{ sort: [property: string] }>()

const active = computed(() => props.sort.property === props.property)
const ariaSort = computed(() =>
  active.value ? (props.sort.direction === 'asc' ? 'ascending' : 'descending') : 'none',
)
</script>

<template>
  <th scope="col" :aria-sort="ariaSort" class="px-2.5 py-3">
    <button
      type="button"
      class="-mx-1 inline-flex items-center gap-1 rounded-md px-1 text-[13px] font-bold text-secondary-text outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
      @click="emit('sort', property)"
    >
      {{ label }}
      <ArrowUpIcon v-if="active && sort.direction === 'asc'" class="size-3.5 text-foreground" aria-hidden="true" />
      <ArrowDownIcon v-else-if="active" class="size-3.5 text-foreground" aria-hidden="true" />
      <ArrowUpDownIcon v-else class="size-3.5 opacity-60" aria-hidden="true" />
    </button>
  </th>
</template>
