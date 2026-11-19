<script lang="ts" setup>
import type { ToasterProps } from 'vue-sonner'

import {
  CircleAlertIcon,
  CircleCheckIcon,
  InfoIcon,
  Loader2Icon,
  TriangleAlertIcon,
  XIcon,
} from '@lucide/vue'
import { reactiveOmit } from '@vueuse/core'
import { cn } from '@/lib/utils'
import { Toaster as Sonner } from 'vue-sonner'

// Dark toasts in both themes ("Warm coral"); part styles live in main.css.
const props = defineProps<ToasterProps>()
const delegatedProps = reactiveOmit(props, 'class', 'toastOptions')
</script>

<template>
  <Sonner
    :class="cn('toaster group', props.class)"
    :style="{
      '--normal-bg': 'var(--toast)',
      '--normal-text': 'var(--toast-foreground)',
      '--normal-border': 'var(--toast)',
      '--border-radius': '14px',
      '--width': '380px',
    }"
    :toast-options="props.toastOptions"
    v-bind="delegatedProps"
  >
    <template #success-icon>
      <CircleCheckIcon class="size-5 text-toast-success" />
    </template>
    <template #info-icon>
      <InfoIcon class="size-5" />
    </template>
    <template #warning-icon>
      <TriangleAlertIcon class="size-5 text-toast-error" />
    </template>
    <template #error-icon>
      <CircleAlertIcon class="size-5 text-toast-error" />
    </template>
    <template #loading-icon>
      <div>
        <Loader2Icon class="size-5 animate-spin" />
      </div>
    </template>
    <template #close-icon>
      <XIcon class="size-4" />
    </template>
  </Sonner>
</template>
