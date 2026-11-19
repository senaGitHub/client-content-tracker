<script setup lang="ts">
import { MoonIcon, SunIcon } from '@lucide/vue'
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { useTheme } from '@/composables/useTheme'

const props = withDefaults(defineProps<{ iconOnly?: boolean }>(), { iconOnly: false })

const { isDark, toggleTheme } = useTheme()
const label = computed(() => (isDark.value ? 'Açık temaya geç' : 'Koyu temaya geç'))
</script>

<template>
  <Button
    v-if="props.iconOnly"
    variant="ghost"
    size="icon"
    :aria-label="label"
    :title="label"
    @click="toggleTheme"
  >
    <SunIcon v-if="isDark" />
    <MoonIcon v-else />
  </Button>
  <button
    v-else
    type="button"
    class="flex w-full items-center gap-3 rounded-lg px-5 py-2 text-sm font-medium text-muted-foreground outline-none transition-colors hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
    :aria-label="label"
    @click="toggleTheme"
  >
    <SunIcon v-if="isDark" class="size-[18px]" aria-hidden="true" />
    <MoonIcon v-else class="size-[18px]" aria-hidden="true" />
    {{ isDark ? 'Açık tema' : 'Koyu tema' }}
  </button>
</template>
