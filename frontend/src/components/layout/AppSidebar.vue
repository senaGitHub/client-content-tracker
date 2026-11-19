<script setup lang="ts">
import type { Component } from 'vue'
import { CircleCheckIcon, HouseIcon, ImageIcon, LayersIcon } from '@lucide/vue'
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRoute } from 'vue-router'
import { useNavCounts } from '@/composables/useNavCounts'
import { cn } from '@/lib/utils'
import BrandMark from './BrandMark.vue'
import ThemeToggle from './ThemeToggle.vue'

interface NavItem {
  key: 'overview' | 'brands' | 'contents' | 'approvals'
  label: string
  to: RouteLocationRaw
  icon: Component
  count?: number
  /** Approvals: a filled coral badge instead of a plain number. */
  badge?: boolean
}

const route = useRoute()
const counts = useNavCounts()

const items = computed<NavItem[]>(() => [
  { key: 'overview', label: 'Genel Bakış', to: '/', icon: HouseIcon },
  { key: 'brands', label: 'Markalar', to: '/clients', icon: LayersIcon, count: counts.value?.brands },
  { key: 'contents', label: 'İçerikler', to: '/content', icon: ImageIcon, count: counts.value?.contents },
  {
    // Not a page of its own: the content list filtered to "Onayda".
    key: 'approvals',
    label: 'Onay Süreci',
    to: { path: '/content', query: { view: 'list', status: 'IN_REVIEW' } },
    icon: CircleCheckIcon,
    count: counts.value?.inReview,
    badge: true,
  },
])

const isApprovalsView = computed(() => route.path === '/content' && route.query.status === 'IN_REVIEW')

function isActive(key: NavItem['key']): boolean {
  switch (key) {
    case 'overview':
      return route.path === '/'
    case 'brands':
      return route.path === '/clients' || route.path.startsWith('/clients/')
    case 'contents':
      return route.path === '/content' && !isApprovalsView.value
    case 'approvals':
      return isApprovalsView.value
  }
}
</script>

<template>
  <div class="flex h-full flex-col gap-[34px] overflow-hidden bg-sidebar px-4 pt-[30px]">
    <BrandMark class="self-start px-3.5" />

    <nav aria-label="Ana menü" class="flex flex-col gap-3">
      <ul class="flex flex-col gap-1.5">
        <li v-for="item in items" :key="item.key">
          <RouterLink
            :to="item.to"
            :aria-current="isActive(item.key) ? 'page' : undefined"
            :class="cn(
              'flex items-center gap-3.5 rounded-xl px-4 py-3 text-base outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
              isActive(item.key)
                ? 'bg-nav-active font-semibold text-coral-text'
                : 'font-medium text-foreground hover:bg-coral-soft',
            )"
          >
            <component :is="item.icon" class="size-[22px] shrink-0" :stroke-width="1.8" aria-hidden="true" />
            <span class="min-w-0 flex-1 truncate">{{ item.label }}</span>
            <template v-if="item.count !== undefined">
              <span
                v-if="item.badge"
                v-show="item.count > 0"
                class="shrink-0 rounded-full bg-coral-solid px-2 py-0.5 text-[13px] font-bold text-coral-solid-foreground tabular-nums"
              >
                <span class="sr-only">, </span>{{ item.count }}<span class="sr-only"> içerik onay bekliyor</span>
              </span>
              <!-- The hidden words make screen readers say "Markalar, 6 marka" instead of "Markalar6". -->
              <span v-else class="shrink-0 text-[13px] font-normal text-muted-foreground tabular-nums">
                <span class="sr-only">, </span>{{ item.count }}<span class="sr-only"> {{ item.key === 'brands' ? 'marka' : 'içerik' }}</span>
              </span>
            </template>
          </RouterLink>
        </li>
      </ul>
      <ThemeToggle />
    </nav>

    <!-- Full-bleed photo; decorative, so the alt text is empty. -->
    <div class="relative -mx-4 mt-auto h-[clamp(140px,38vh,360px)] shrink-0">
      <img
        src="/images/sidebar-pool-edge.jpg"
        alt=""
        class="absolute inset-0 size-full object-cover"
        decoding="async"
      />
      <!-- Dark scrim so the pill stays readable on any part of the photo. -->
      <div class="absolute inset-x-0 bottom-0 h-24 bg-linear-to-t from-black/45 to-transparent" aria-hidden="true" />
      <span class="absolute bottom-[30px] left-[30px] rounded-full border border-white/75 px-3 py-1 text-[13px] text-white">
        Demo veriler
      </span>
    </div>
  </div>
</template>
