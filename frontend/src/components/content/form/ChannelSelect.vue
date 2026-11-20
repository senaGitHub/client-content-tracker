<script setup lang="ts">
import { computed } from 'vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { CHANNEL_SUGGESTIONS } from '@/composables/useContentForm'

/**
 * The Kanal field of the New content dialog and the Edit sheet: one channel from a fixed list, or
 * "Kanal yok" (the field is optional). The value is the channel text ('' = none), as before.
 *
 * id, aria-invalid and aria-describedby fall through to the trigger, so the form's <Label for> and
 * its error message keep working.
 */
defineOptions({ inheritAttrs: false })

const channel = defineModel<string>({ required: true })

// Select items can't have an empty value, so "no channel" is a sentinel.
const NONE = '__none__'

/**
 * The fixed list. A channel saved earlier that is not in it (older or imported data) is added at
 * the top, so editing that content shows it selected instead of losing it.
 */
const options = computed(() => {
  const current = channel.value.trim()
  return current && !CHANNEL_SUGGESTIONS.includes(current) ? [current, ...CHANNEL_SUGGESTIONS] : CHANNEL_SUGGESTIONS
})
</script>

<template>
  <Select
    :model-value="channel.trim() ? channel.trim() : NONE"
    @update:model-value="(v) => (channel = v === NONE || v == null ? '' : String(v))"
  >
    <SelectTrigger v-bind="$attrs" class="w-full">
      <SelectValue />
    </SelectTrigger>
    <SelectContent>
      <SelectItem :value="NONE">Kanal yok</SelectItem>
      <SelectItem v-for="option in options" :key="option" :value="option">{{ option }}</SelectItem>
    </SelectContent>
  </Select>
</template>
