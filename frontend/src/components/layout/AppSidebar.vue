<script setup lang="ts">
import type { Component } from 'vue'
import { CircleCheckIcon, HouseIcon, ImageIcon, LayersIcon } from '@lucide/vue'
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRoute } from 'vue-router'
import { useNavCounts } from '@/composables/useNavCounts'
import { cn } from '@/lib/utils'
import BrandMark from './BrandMark.vue'

interface NavItem {
  key: 'overview' | 'brands' | 'contents' | 'approvals'
  label: string
  to: RouteLocationRaw
  icon: Component
  /** Only Onay Süreci has one: a filled coral badge, hidden at 0. */
  badge?: number
}

const route = useRoute()
const counts = useNavCounts()

const items = computed<NavItem[]>(() => [
  { key: 'overview', label: 'Genel Bakış', to: '/', icon: HouseIcon },
  { key: 'brands', label: 'Markalar', to: '/clients', icon: LayersIcon },
  { key: 'contents', label: 'İçerikler', to: '/content', icon: ImageIcon },
  {
    // Not a page of its own: the content list filtered to "Onayda".
    key: 'approvals',
    label: 'Onay Süreci',
    to: { path: '/content', query: { view: 'list', status: 'IN_REVIEW' } },
    icon: CircleCheckIcon,
    badge: counts.value?.inReview,
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
              'flex items-center gap-3 rounded-xl px-4 py-2.5 text-nav outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
              isActive(item.key)
                ? 'bg-nav-active font-semibold text-coral-text'
                : 'text-foreground hover:bg-coral-soft',
            )"
          >
            <component :is="item.icon" class="size-[18px] shrink-0" :stroke-width="1.8" aria-hidden="true" />
            <span class="min-w-0 flex-1 truncate">{{ item.label }}</span>
            <span
              v-if="item.badge"
              class="shrink-0 rounded-full bg-coral-solid px-2 py-0.5 text-[13px] font-bold text-coral-solid-foreground tabular-nums"
            >
              <span class="sr-only">, </span>{{ item.badge }}<span class="sr-only"> içerik onay bekliyor</span>
            </span>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <!-- A quiet reminder that the data is fictional, pinned to the bottom. -->
    <p class="mt-auto px-3.5 pb-6">
      <span class="inline-block rounded-full border px-2.5 py-0.5 text-xs text-muted-foreground">Demo veriler</span>
    </p>
  </div>
</template>
