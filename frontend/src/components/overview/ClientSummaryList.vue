<script setup lang="ts">
import type { ClientResponse } from '@/api'
import PageSection from '@/components/common/PageSection.vue'
import TextLink from '@/components/common/TextLink.vue'
import { Skeleton } from '@/components/ui/skeleton'
import { clientLink } from '@/lib/content'

defineProps<{ clients?: ClientResponse[]; loading?: boolean }>()
</script>

<template>
  <PageSection title="Clients" description="Where each brand's content stands.">
    <template #actions>
      <TextLink to="/clients">All clients</TextLink>
    </template>

    <ul v-if="loading || !clients" class="divide-y rounded-xl border" aria-hidden="true">
      <li v-for="n in 5" :key="n" class="flex items-center justify-between gap-4 px-4 py-3.5">
        <span class="flex flex-col gap-1.5">
          <Skeleton class="h-3.5 w-36" />
          <Skeleton class="h-3 w-20" />
        </span>
        <Skeleton class="h-4 w-28" />
      </li>
    </ul>

    <p v-else-if="clients.length === 0" class="rounded-xl border border-dashed px-4 py-8 text-center text-sm text-muted-foreground">
      No clients yet.
    </p>

    <ul v-else class="divide-y rounded-xl border">
      <li v-for="client in clients" :key="client.id">
        <RouterLink
          :to="clientLink(client.id)"
          class="flex flex-col gap-2 px-4 py-3 outline-none transition-colors first:rounded-t-xl last:rounded-b-xl hover:bg-muted/50 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:ring-inset sm:flex-row sm:items-center sm:justify-between sm:gap-4"
        >
          <span class="min-w-0">
            <span class="block truncate text-sm font-medium text-foreground">{{ client.name }}</span>
            <span class="block truncate text-xs text-muted-foreground">{{ client.industry }}</span>
          </span>

          <span class="flex shrink-0 flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground tabular-nums">
            <span class="inline-flex items-center gap-1.5" :title="`${client.counts.draft} draft`">
              <span class="size-1.5 rounded-full bg-draft-dot" aria-hidden="true" />
              {{ client.counts.draft }}<span class="sr-only"> draft</span>
            </span>
            <span class="inline-flex items-center gap-1.5" :title="`${client.counts.inReview} in review`">
              <span class="size-1.5 rounded-full bg-review-dot" aria-hidden="true" />
              {{ client.counts.inReview }}<span class="sr-only"> in review</span>
            </span>
            <span class="inline-flex items-center gap-1.5" :title="`${client.counts.published} published`">
              <span class="size-1.5 rounded-full bg-published-dot" aria-hidden="true" />
              {{ client.counts.published }}<span class="sr-only"> published</span>
            </span>
            <span v-if="client.counts.overdue > 0" class="font-medium text-overdue">
              {{ client.counts.overdue }} overdue
            </span>
          </span>
        </RouterLink>
      </li>
    </ul>
  </PageSection>
</template>
