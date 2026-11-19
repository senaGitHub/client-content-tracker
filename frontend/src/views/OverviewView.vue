<script setup lang="ts">
import PageHeader from '@/components/layout/PageHeader.vue'
import PlaceholderPanel from '@/components/layout/PlaceholderPanel.vue'
import { formatLongDate } from '@/lib/format'

const today = formatLongDate()
</script>

<template>
  <PageHeader title="Genel Bakış" :description="today" />
  <PlaceholderPanel text="Yeni Genel Bakış (özet kartları, markalar ve onay paneli) Faz 10'da geliyor." />
</template>
