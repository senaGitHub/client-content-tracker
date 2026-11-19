<script setup lang="ts">
import { useId } from 'vue'

defineProps<{ title: string; description?: string }>()

const headingId = useId()
</script>

<template>
  <section :aria-labelledby="headingId" class="flex min-w-0 flex-col gap-4">
    <div class="flex flex-wrap items-end justify-between gap-x-4 gap-y-1">
      <div class="min-w-0">
        <h2 :id="headingId" class="text-base font-semibold tracking-tight text-foreground">{{ title }}</h2>
        <p v-if="description" class="mt-0.5 text-sm text-muted-foreground">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="flex shrink-0 items-center gap-2">
        <slot name="actions" />
      </div>
    </div>
    <slot />
  </section>
</template>
