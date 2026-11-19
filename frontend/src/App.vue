<script setup lang="ts">
import { Toaster } from '@/components/ui/sonner'
</script>

<template>
  <RouterView />
  <!-- Toasts are dark in both themes, so the toaster always uses sonner's dark base. -->
  <Toaster theme="dark" position="bottom-right" :visible-toasts="4" />
</template>
