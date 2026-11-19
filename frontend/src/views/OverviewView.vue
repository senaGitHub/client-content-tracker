<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { toast } from 'vue-sonner'
import { ApiError, dashboardApi } from '@/api'
import type { StatusCounts } from '@/api'
import PageHeader from '@/components/layout/PageHeader.vue'
import PlaceholderPanel from '@/components/layout/PlaceholderPanel.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

// Phase 5: a minimal API round-trip so the client, CORS and env config are verified.
// The real overview (needs attention, week, clients, activity) replaces this in Phase 6.
const totals = ref<StatusCounts | null>(null)
const clientCount = ref(0)
const loading = ref(true)
const error = ref<string | null>(null)

let controller: AbortController | null = null

async function load() {
  controller?.abort()
  controller = new AbortController()
  loading.value = true
  error.value = null
  try {
    const data = await dashboardApi.get(controller.signal)
    totals.value = data.totals
    clientCount.value = data.clients.length
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = e instanceof ApiError ? e.message : 'Something went wrong.'
    toast.error('Could not load the overview', { description: error.value })
  } finally {
    loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <PageHeader title="Overview" description="What needs your attention across all clients." />

  <section aria-live="polite" class="mb-6 rounded-xl border p-5">
    <div v-if="loading" class="flex flex-col gap-2">
      <Skeleton class="h-4 w-40" />
      <Skeleton class="h-4 w-64" />
    </div>
    <div v-else-if="error" class="flex flex-wrap items-center justify-between gap-3">
      <p class="text-sm text-muted-foreground">{{ error }}</p>
      <Button variant="outline" size="sm" @click="load">Try again</Button>
    </div>
    <div v-else-if="totals" class="text-sm">
      <p class="font-medium text-foreground">Connected to the Harbour API</p>
      <p class="mt-1 text-muted-foreground">
        {{ totals.total }} content items across {{ clientCount }} clients:
        {{ totals.draft }} draft, {{ totals.inReview }} in review, {{ totals.published }} published,
        <span class="text-overdue">{{ totals.overdue }} overdue</span>.
      </p>
    </div>
  </section>

  <PlaceholderPanel text="Needs attention, this week, clients and recent activity are built in the next phase." />
</template>
