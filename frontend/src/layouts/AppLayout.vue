<script setup lang="ts">
import { MenuIcon } from '@lucide/vue'
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ContentFormDialog from '@/components/content/ContentFormDialog.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import BrandMark from '@/components/layout/BrandMark.vue'
import ThemeToggle from '@/components/layout/ThemeToggle.vue'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'

const mobileNavOpen = ref(false)
const route = useRoute()

// Close the mobile drawer after navigating.
watch(() => route.fullPath, () => {
  mobileNavOpen.value = false
})
</script>

<template>
  <div class="flex min-h-dvh flex-col bg-background md:flex-row">
    <a
      href="#main"
      class="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-lg focus:bg-card focus:px-3 focus:py-2 focus:text-sm focus:ring-3 focus:ring-ring/50"
    >
      Skip to content
    </a>

    <!-- Desktop sidebar: sits on the page background, next to the surface panel. -->
    <aside class="sticky top-0 hidden h-dvh w-64 shrink-0 px-4 py-6 md:block">
      <AppSidebar />
    </aside>

    <!-- Mobile top bar with a slide-in navigation drawer. -->
    <header class="sticky top-0 z-30 flex h-14 items-center gap-2 bg-background px-3 md:hidden">
      <Sheet v-model:open="mobileNavOpen">
        <SheetTrigger as-child>
          <Button variant="ghost" size="icon" aria-label="Open navigation">
            <MenuIcon />
          </Button>
        </SheetTrigger>
        <SheetContent side="left" class="w-72 border-border bg-background px-4 py-6 shadow-none">
          <SheetTitle class="sr-only">Navigation</SheetTitle>
          <SheetDescription class="sr-only">Move between Harbour pages.</SheetDescription>
          <AppSidebar />
        </SheetContent>
      </Sheet>
      <BrandMark compact />
      <ThemeToggle icon-only class="ml-auto" />
    </header>

    <main id="main" class="flex min-w-0 flex-1 flex-col px-2 pb-2 md:py-3 md:pr-3 md:pl-0">
      <div class="flex-1 rounded-xl border bg-card">
        <div class="mx-auto w-full max-w-6xl px-5 py-6 sm:px-8 md:py-10 lg:px-10">
          <RouterView />
        </div>
      </div>
    </main>

    <!-- One shared New / Edit content dialog; pages open it via useContentDialog(). -->
    <ContentFormDialog />
  </div>
</template>
