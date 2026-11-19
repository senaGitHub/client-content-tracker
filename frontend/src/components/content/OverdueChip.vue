<script setup lang="ts">
import { overdueLabel } from '@/lib/format'

// The one solid red element: "2 gün gecikti". Only render it when the server says `overdue`.
defineProps<{ dueDate: string }>()
</script>

<template>
  <span class="inline-flex shrink-0 items-center rounded-sm bg-overdue px-2 py-0.5 text-xs font-semibold whitespace-nowrap text-white">
    {{ overdueLabel(dueDate) }}
  </span>
</template>
