<script setup lang="ts">
import { MailIcon } from '@lucide/vue'
import type { ClientResponse } from '@/api'
import BrandCover from '@/components/client/BrandCover.vue'
import { vHighlight } from '@/composables/useHighlight'
import { clientLink } from '@/lib/content'
import { cn } from '@/lib/utils'

defineProps<{ client: ClientResponse }>()
</script>

<template>
  <!--
    The whole card opens the brand, but the mail button is a separate link: a link inside a link is
    invalid HTML, so the brand link is an overlay over the whole card and the mail link sits above it.
  -->
  <article v-highlight:client="client.id" class="group relative flex flex-col overflow-hidden rounded-card border bg-card shadow-card transition-shadow hover:shadow-pop">
    <RouterLink
      :to="clientLink(client.id)"
      class="absolute inset-0 z-[1] rounded-card outline-none focus-visible:ring-3 focus-visible:ring-ring/60 focus-visible:ring-inset"
    >
      <span class="sr-only">{{ client.name }}</span>
    </RouterLink>
    <div class="relative h-[140px] shrink-0 overflow-hidden">
      <BrandCover :name="client.name" :url="client.coverImageUrl" zoom class="absolute inset-0 size-full" initials-class="-mt-8 text-[44px]" />
      <!-- Dark scrim only over photos, so the white name stays readable. -->
      <span v-if="client.coverImageUrl" class="absolute inset-0 bg-linear-to-b from-transparent from-45% to-black/70" aria-hidden="true" />
      <div :class="cn('absolute right-4 bottom-3.5 left-[18px]', client.coverImageUrl ? 'text-white' : 'text-foreground')">
        <h2 class="truncate text-[22px] leading-tight font-bold">{{ client.name }}</h2>
        <p :class="cn('truncate text-sm', client.coverImageUrl ? 'opacity-90' : 'text-secondary-text')">{{ client.industry }}</p>
      </div>
    </div>

    <div class="flex flex-1 flex-col gap-3 px-[18px] py-4">
      <!-- min-h-6 = badge height, so cards without badges keep the same row height. -->
      <div class="flex min-h-6 flex-wrap items-center gap-2">
        <span v-if="client.counts.overdue" class="rounded-full bg-overdue px-2.5 py-[3px] text-xs font-semibold text-white">
          {{ client.counts.overdue }} gecikmiş
        </span>
        <span v-if="client.counts.inReview" class="rounded-full bg-review px-2.5 py-[3px] text-xs font-semibold text-review-foreground">
          {{ client.counts.inReview }} onayda
        </span>
        <span v-if="!client.counts.overdue && !client.counts.inReview" class="text-xs text-muted-foreground">Bekleyen iş yok</span>
        <span class="ml-auto text-[13px] text-muted-foreground tabular-nums">{{ client.counts.total }} içerik</span>
      </div>

      <div class="mt-auto flex items-center justify-between gap-3 border-t border-divider pt-3">
        <div class="min-w-0">
          <p class="truncate text-sm font-semibold">{{ client.contactName }}</p>
          <p class="truncate text-[13px] text-muted-foreground">{{ client.contactEmail }}</p>
        </div>
        <a
          :href="`mailto:${client.contactEmail}`"
          class="relative z-[2] flex size-9 shrink-0 items-center justify-center rounded-[10px] border border-input bg-card text-coral outline-none transition-colors hover:bg-coral-soft focus-visible:ring-3 focus-visible:ring-ring/50"
          :aria-label="`${client.contactName} kişisine e-posta gönder`"
          :title="`E-posta: ${client.contactEmail}`"
        >
          <MailIcon class="size-4" :stroke-width="1.8" aria-hidden="true" />
        </a>
      </div>
    </div>
  </article>
</template>
