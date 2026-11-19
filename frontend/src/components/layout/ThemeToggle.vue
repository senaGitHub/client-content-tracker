<script setup lang="ts">
import { MoonIcon, SunIcon } from '@lucide/vue'
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { useTheme } from '@/composables/useTheme'

const props = withDefaults(defineProps<{ iconOnly?: boolean }>(), { iconOnly: false })

const { isDark, toggleTheme } = useTheme()
const label = computed(() => (isDark.value ? 'Switch to light theme' : 'Switch to dark theme'))
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
  <Button
    v-else
    variant="ghost"
    class="h-9 w-full justify-start gap-3 px-3 text-muted-foreground hover:text-foreground"
    :aria-label="label"
    @click="toggleTheme"
  >
    <SunIcon v-if="isDark" />
    <MoonIcon v-else />
    {{ isDark ? 'Light mode' : 'Dark mode' }}
  </Button>
</template>
