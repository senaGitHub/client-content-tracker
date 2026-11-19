<script setup lang="ts">
// Temporary stand-in for pages that are built in later phases.
defineProps<{ text: string }>()
</script>

<template>
  <div class="flex min-h-48 items-center justify-center rounded-xl border border-dashed p-8 text-center">
    <p class="max-w-sm text-sm text-muted-foreground">{{ text }}</p>
  </div>
</template>
