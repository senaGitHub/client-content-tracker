<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import ThemeToggle from './ThemeToggle.vue'

const props = defineProps<{ title: string; description?: string }>()

/** Round capitals (O, Ö, C, Ç, G, Ğ, Q) look further in than their box, so they get an extra -0.04em. */
const ROUND_START = /^[OÖCÇGĞQ]/
const opticalOffset = computed(() => (ROUND_START.test(props.title.trim()) ? 'ml-[-0.06em]' : 'ml-[-0.02em]'))
</script>

<template>
  <header class="mb-6 flex flex-wrap items-center justify-between gap-4">
    <div class="min-w-0">
      <!--
        On desktop the title line is 32px tall, like the sidebar's logo row, so the two line up.
        Left offset: optical alignment with the description below. At 24px bold, Inter's first letters
        sit ~0.05–0.8px further right than the 14px description's (glyph xMin: O 1.10 vs 0.83px,
        M 1.58 vs 0.83, İ 1.45 vs 0.67); 0.02em (~0.5px) is the middle of that range. Round first letters (O, G, C...) read as further in
        than their bounding box, so they get -0.06em in total.
      -->
      <h1 :class="cn('text-page-title text-foreground lg:flex lg:min-h-8 lg:items-center', opticalOffset)">
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
