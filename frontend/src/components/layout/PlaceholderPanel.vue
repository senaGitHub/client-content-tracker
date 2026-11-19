<script setup lang="ts">
// Temporary stand-in for pages that are built in later phases.
defineProps<{ text: string }>()
</script>

<template>
  <div class="flex min-h-48 items-center justify-center rounded-card border border-dashed border-input bg-card p-8 text-center shadow-card">
    <p class="max-w-sm text-sm text-muted-foreground">{{ text }}</p>
  </div>
</template>
