<script setup lang="ts">
import ThemeToggle from './ThemeToggle.vue'

defineProps<{ title: string; description?: string }>()
</script>

<template>
  <header class="mb-6 flex flex-wrap items-center justify-between gap-4">
    <div class="min-w-0">
      <h1 class="text-page-title text-foreground">
        {{ title }}
      </h1>
      <p v-if="description || $slots.description" class="mt-1 text-sm text-secondary-text">
        <slot name="description">{{ description }}</slot>
      </p>
    </div>
    <div class="flex flex-wrap items-center gap-3">
      <slot name="actions" />
      <!-- Desktop only: on smaller screens the theme toggle lives in the top bar (AppLayout). -->
      <ThemeToggle class="hidden lg:inline-flex" />
    </div>
  </header>
</template>
