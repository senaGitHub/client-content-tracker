<script setup lang="ts">
import { MoonIcon, SunIcon } from '@lucide/vue'
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { useTheme } from '@/composables/useTheme'

// The provider renders no element of its own, so classes from the parent go to the button.
defineOptions({ inheritAttrs: false })

const { isDark, toggleTheme } = useTheme()
const label = computed(() => (isDark.value ? 'Açık temaya geç' : 'Koyu temaya geç'))
</script>

<template>
  <!-- Icon only: the aria-label names it for screen readers, the tooltip for sighted users. -->
  <TooltipProvider :delay-duration="300">
    <Tooltip>
      <TooltipTrigger as-child>
        <Button v-bind="$attrs" variant="ghost" size="icon" :aria-label="label" @click="toggleTheme">
          <SunIcon v-if="isDark" aria-hidden="true" />
          <MoonIcon v-else aria-hidden="true" />
        </Button>
      </TooltipTrigger>
      <TooltipContent side="bottom">{{ label }}</TooltipContent>
    </Tooltip>
  </TooltipProvider>
</template>
