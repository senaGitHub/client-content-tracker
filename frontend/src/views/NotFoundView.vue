<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <div class="flex min-h-[50vh] flex-col items-center justify-center gap-3 text-center">
    <p class="text-sm font-medium text-primary">404</p>
    <h1 class="text-2xl font-semibold tracking-tight">Page not found</h1>
    <p class="max-w-sm text-sm text-muted-foreground">
      The page you are looking for does not exist or has moved.
    </p>
    <Button as-child class="mt-2">
      <RouterLink to="/">Back to overview</RouterLink>
    </Button>
  </div>
</template>
