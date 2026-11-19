<script setup lang="ts">
import { XIcon } from '@lucide/vue'
import { computed } from 'vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'

/**
 * A filter dropdown that shows its state: "Tüm Markalar" when unset; when set, it is tinted and
 * gets a "×" that clears it (as in the mockups' "Teslim: Geciken ×").
 */
const props = withDefaults(
  defineProps<{
    /** Undefined = no filter. */
    modelValue?: string
    options: { value: string; label: string }[]
    /** Shown when no filter is set, and as the first option. */
    allLabel: string
    /** Prepended to the selected label, e.g. "Teslim: ". */
    prefix?: string
    label: string
    /** 'overdue' tints the active state red (only for the overdue filter). */
    tone?: 'coral' | 'overdue'
  }>(),
  { prefix: '', tone: 'coral' },
)
const emit = defineEmits<{ 'update:modelValue': [value: string | undefined] }>()

// Select items can't have an empty value, so "no filter" is the 'all' sentinel.
const ALL = '__all__'

const active = computed(() => props.modelValue !== undefined)
const display = computed(() => {
  const option = props.options.find((o) => o.value === props.modelValue)
  return option ? `${props.prefix}${option.label}` : props.allLabel
})
</script>

<template>
  <div
    :class="cn(
      'flex h-10 min-w-0 items-center rounded-lg border transition-colors focus-within:ring-3 focus-within:ring-ring/50',
      !active && 'border-input bg-card text-foreground',
      active && tone === 'coral' && 'border-nav-active bg-coral-soft text-coral-text',
      active && tone === 'overdue' && 'border-overdue-border bg-overdue-soft text-overdue-text',
    )"
  >
    <Select
      :model-value="modelValue ?? ALL"
      @update:model-value="(v) => emit('update:modelValue', v === ALL || v == null ? undefined : String(v))"
    >
      <SelectTrigger
        :aria-label="label"
        :class="cn(
          'h-full min-w-0 flex-1 border-0 bg-transparent px-3.5 text-sm font-medium text-inherit shadow-none focus-visible:ring-0',
          active && 'pr-1 [&>svg:last-child]:hidden',
        )"
      >
        <SelectValue>{{ display }}</SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectItem :value="ALL">{{ allLabel }}</SelectItem>
        <SelectItem v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</SelectItem>
      </SelectContent>
    </Select>
    <button
      v-if="active"
      type="button"
      class="mr-1.5 flex size-7 shrink-0 items-center justify-center rounded-md outline-none hover:bg-black/5 focus-visible:ring-3 focus-visible:ring-ring/50 dark:hover:bg-white/10"
      :aria-label="`${label} filtresini kaldır`"
      @click="emit('update:modelValue', undefined)"
    >
      <XIcon class="size-4" aria-hidden="true" />
    </button>
  </div>
</template>
