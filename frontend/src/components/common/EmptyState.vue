<script setup lang="ts">
import type { Component } from 'vue'
import { InboxIcon } from '@lucide/vue'

withDefaults(defineProps<{ title: string; description?: string; icon?: Component }>(), {
  icon: () => InboxIcon,
})
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed px-6 py-12 text-center">
    <span class="mb-1 flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground">
      <component :is="icon" class="size-5" aria-hidden="true" />
    </span>
    <p class="text-sm font-medium text-foreground">{{ title }}</p>
    <p v-if="description" class="max-w-sm text-sm text-muted-foreground">{{ description }}</p>
    <div v-if="$slots.default" class="mt-3">
      <slot />
    </div>
  </div>
</template>
