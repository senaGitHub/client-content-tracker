<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>

<template>
  <header class="mb-6 flex flex-wrap items-center justify-between gap-4">
    <div class="min-w-0">
      <h1 class="text-[28px] leading-tight font-extrabold tracking-[-0.025em] text-foreground sm:text-[30px]">
        {{ title }}
      </h1>
      <p v-if="description || $slots.description" class="mt-1 text-base text-secondary-text">
        <slot name="description">{{ description }}</slot>
      </p>
    </div>
    <div v-if="$slots.actions" class="flex flex-wrap items-center gap-3">
      <slot name="actions" />
    </div>
  </header>
</template>
