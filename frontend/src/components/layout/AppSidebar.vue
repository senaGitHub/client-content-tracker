<script setup lang="ts">
import type { Component } from 'vue'
import { FileTextIcon, LayoutDashboardIcon, UsersIcon } from '@lucide/vue'
import { useRoute } from 'vue-router'
import { cn } from '@/lib/utils'
import BrandMark from './BrandMark.vue'
import ThemeToggle from './ThemeToggle.vue'

interface NavItem {
  label: string
  to: string
  icon: Component
}

const items: NavItem[] = [
  { label: 'Overview', to: '/', icon: LayoutDashboardIcon },
  { label: 'Clients', to: '/clients', icon: UsersIcon },
  { label: 'Content', to: '/content', icon: FileTextIcon },
]

const route = useRoute()

// "/" must match exactly; the others also stay active on nested pages (e.g. /clients/3).
function isActive(to: string) {
  return to === '/' ? route.path === '/' : route.path === to || route.path.startsWith(`${to}/`)
}
</script>

<template>
  <div class="flex h-full flex-col gap-8">
    <BrandMark class="px-1" />

    <nav aria-label="Main">
      <ul class="flex flex-col gap-1">
        <li v-for="item in items" :key="item.to">
          <RouterLink
            :to="item.to"
            :aria-current="isActive(item.to) ? 'page' : undefined"
            :class="cn(
              'flex h-9 items-center gap-3 rounded-lg border px-3 text-sm font-medium outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
              isActive(item.to)
                ? 'border-border bg-card text-foreground'
                : 'border-transparent text-muted-foreground hover:bg-muted hover:text-foreground',
            )"
          >
            <component
              :is="item.icon"
              :class="cn('size-4 shrink-0', isActive(item.to) && 'text-primary')"
              aria-hidden="true"
            />
            {{ item.label }}
          </RouterLink>
        </li>
      </ul>
    </nav>

    <div class="mt-auto">
      <ThemeToggle />
    </div>
  </div>
</template>
