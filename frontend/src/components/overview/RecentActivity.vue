<script setup lang="ts">
import type { Component } from 'vue'
import { ArrowRightIcon, PencilIcon, PlusIcon } from '@lucide/vue'
import type { ActivityAction, ActivityResponse } from '@/api'
import PageSection from '@/components/common/PageSection.vue'
import StatusBadge from '@/components/content/StatusBadge.vue'
import { Skeleton } from '@/components/ui/skeleton'
import { clientLink } from '@/lib/content'
import { formatDateTime, formatRelative } from '@/lib/format'

defineProps<{ items?: ActivityResponse[]; loading?: boolean }>()

const ICONS: Record<ActivityAction, Component> = {
  CREATED: PlusIcon,
  UPDATED: PencilIcon,
  STATUS_CHANGED: ArrowRightIcon,
}
</script>

<template>
  <PageSection title="Recent activity" description="The latest changes across all clients.">
    <ol v-if="loading || !items" class="flex flex-col gap-4 rounded-xl border p-4" aria-hidden="true">
      <li v-for="n in 5" :key="n" class="flex gap-3">
        <Skeleton class="size-7 shrink-0 rounded-full" />
        <span class="flex flex-1 flex-col gap-1.5">
          <Skeleton class="h-3.5 w-full" />
          <Skeleton class="h-3 w-1/2" />
        </span>
      </li>
    </ol>

    <p v-else-if="items.length === 0" class="rounded-xl border border-dashed px-4 py-8 text-center text-sm text-muted-foreground">
      No activity yet.
    </p>

    <ol v-else class="flex flex-col gap-4 rounded-xl border p-4">
      <li v-for="entry in items" :key="entry.id" class="flex gap-3">
        <span class="flex size-7 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground">
          <component :is="ICONS[entry.action]" class="size-3.5" aria-hidden="true" />
        </span>
        <div class="min-w-0 flex-1 text-sm">
          <p class="text-foreground">
            <span class="font-medium">{{ entry.content.title }}</span>
            <template v-if="entry.action === 'CREATED'"> was created</template>
            <template v-else-if="entry.action === 'UPDATED'"> was edited</template>
            <template v-else-if="entry.fromStatus && entry.toStatus">
              moved
              <span class="inline-flex flex-wrap items-center gap-1 align-middle">
                <StatusBadge :status="entry.fromStatus" size="sm" />
                <ArrowRightIcon class="size-3 text-muted-foreground" aria-hidden="true" />
                <span class="sr-only">to</span>
                <StatusBadge :status="entry.toStatus" size="sm" />
              </span>
            </template>
          </p>
          <p class="mt-1 flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
            <RouterLink
              :to="clientLink(entry.client.id)"
              class="truncate rounded-sm outline-none hover:text-foreground hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              {{ entry.client.name }}
            </RouterLink>
            <span aria-hidden="true">·</span>
            <time :datetime="entry.createdAt" :title="formatDateTime(entry.createdAt)" class="shrink-0">
              {{ formatRelative(entry.createdAt) }}
            </time>
          </p>
        </div>
      </li>
    </ol>
  </PageSection>
</template>
