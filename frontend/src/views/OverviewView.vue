<script setup lang="ts">
import { CircleAlertIcon, FileTextIcon } from '@lucide/vue'
import { computed } from 'vue'
import { dashboardApi } from '@/api'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import ClientSummaryList from '@/components/overview/ClientSummaryList.vue'
import NeedsAttention from '@/components/overview/NeedsAttention.vue'
import RecentActivity from '@/components/overview/RecentActivity.vue'
import StatusSummary from '@/components/overview/StatusSummary.vue'
import WeekStrip from '@/components/overview/WeekStrip.vue'
import { Button } from '@/components/ui/button'
import { useAsyncData } from '@/composables/useAsyncData'
import { formatLongDate } from '@/lib/format'

// One request feeds the whole page; every section renders its own skeleton meanwhile.
const { data, error, loading, reload } = useAsyncData((signal) => dashboardApi.get(signal), {
  errorToast: 'Could not load the overview',
})

const today = formatLongDate()
const failed = computed(() => !!error.value && !data.value)
const isEmpty = computed(() => !!data.value && data.value.totals.total === 0)
</script>

<template>
  <PageHeader title="Overview" :description="today" />

  <div v-if="failed" role="alert" class="flex flex-col items-start gap-3 rounded-xl border p-5 sm:flex-row sm:items-center sm:justify-between">
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-overdue" aria-hidden="true" />
      <span><span class="font-medium text-foreground">The overview could not be loaded.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="reload">
      {{ loading ? 'Retrying…' : 'Try again' }}
    </Button>
  </div>

  <EmptyState
    v-else-if="isEmpty"
    :icon="FileTextIcon"
    title="No content yet"
    description="Once content is planned for your clients, deadlines and approvals show up here."
  >
    <Button as-child variant="outline">
      <RouterLink to="/clients">Go to clients</RouterLink>
    </Button>
  </EmptyState>

  <div v-else class="flex flex-col gap-10" :aria-busy="loading">
    <StatusSummary :totals="data?.totals" :loading="loading" />
    <NeedsAttention :data="data?.needsAttention" :loading="loading" />
    <WeekStrip :days="data?.week" :loading="loading" />
    <div class="grid gap-10 lg:grid-cols-3 lg:gap-8">
      <ClientSummaryList class="lg:col-span-2" :clients="data?.clients" :loading="loading" />
      <RecentActivity :items="data?.recentActivity" :loading="loading" />
    </div>
  </div>
</template>
