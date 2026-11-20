<script setup lang="ts">
import { computed } from 'vue'
import type { ClientResponse } from '@/api'
import BrandCover from '@/components/client/BrandCover.vue'
import { Skeleton } from '@/components/ui/skeleton'
import { vHighlight } from '@/composables/useHighlight'
import { clientLink } from '@/lib/content'
import { cn } from '@/lib/utils'

const props = defineProps<{ clients?: ClientResponse[] }>()

const SHOWN = 4

/** The brands that need attention first: most overdue, then most waiting for approval, then the busiest. */
const featured = computed(() =>
  [...(props.clients ?? [])]
    .sort(
      (a, b) =>
        b.counts.overdue - a.counts.overdue ||
        b.counts.inReview - a.counts.inReview ||
        b.counts.total - a.counts.total,
    )
    .slice(0, SHOWN),
)
</script>

<template>
  <section aria-labelledby="overview-brands">
    <div class="mb-2.5 flex items-center justify-between">
      <h2 id="overview-brands" class="text-section-title">Markalara bakış</h2>
      <RouterLink to="/clients" class="rounded-sm text-[13px] font-semibold text-coral-text outline-none hover:underline hover:underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50">
        Tüm markalar →
      </RouterLink>
    </div>

    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 2xl:grid-cols-4">
      <template v-if="!clients">
        <Skeleton v-for="n in SHOWN" :key="n" class="h-[120px] rounded-2xl" />
      </template>

      <RouterLink
        v-for="client in featured"
        v-else
        :key="client.id"
        v-highlight:client="client.id"
        :to="clientLink(client.id)"
        :class="cn(
          'group relative block h-[120px] overflow-hidden rounded-2xl outline-none focus-visible:ring-3 focus-visible:ring-ring/60',
          client.coverImageUrl ? 'text-white' : 'text-foreground',
        )"
      >
        <!-- The photo, or (without one) the same coral-soft tile with initials as the brand card and hero. -->
        <BrandCover
          :name="client.name"
          :url="client.coverImageUrl"
          zoom
          class="absolute inset-0 size-full"
          initials-class="mt-3.5 mr-4 ml-auto self-start text-[34px] opacity-70"
        />
        <!-- Dark scrim only over photos, so the white name and chips stay readable. -->
        <span
          v-if="client.coverImageUrl"
          class="absolute inset-0 bg-linear-to-b from-transparent from-30% to-black/70"
          aria-hidden="true"
        />
        <span class="absolute right-3.5 bottom-3 left-4">
          <span class="block truncate text-[17px] font-bold">{{ client.name }}</span>
          <span class="mt-2 flex flex-wrap items-center gap-1.5">
            <span v-if="client.counts.overdue" class="rounded-full bg-overdue px-2.5 py-0.5 text-xs font-semibold text-white">
              {{ client.counts.overdue }} gecikmiş
            </span>
            <span
              v-if="client.counts.inReview"
              :class="cn(
                'rounded-full px-2.5 py-0.5 text-xs font-semibold',
                client.coverImageUrl ? 'bg-white/90 text-black/85' : 'bg-review text-review-foreground',
              )"
            >
              {{ client.counts.inReview }} onayda
            </span>
            <span :class="cn('ml-auto text-xs', client.coverImageUrl ? 'opacity-90' : 'text-secondary-text')">
              {{ client.counts.total }} içerik
            </span>
          </span>
        </span>
      </RouterLink>
    </div>
  </section>
</template>
