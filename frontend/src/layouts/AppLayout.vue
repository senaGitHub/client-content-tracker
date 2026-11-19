<script setup lang="ts">
import { MenuIcon } from '@lucide/vue'
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import NewClientDialog from '@/components/client/NewClientDialog.vue'
import ContentEditSheet from '@/components/content/ContentEditSheet.vue'
import NewContentDialog from '@/components/content/NewContentDialog.vue'
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
  <div class="flex min-h-dvh flex-col bg-background lg:flex-row">
    <a
      href="#main"
      class="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-lg focus:bg-card focus:px-3 focus:py-2 focus:text-sm focus:shadow-pop focus:ring-3 focus:ring-ring/50"
    >
      İçeriğe geç
    </a>

    <!-- Desktop sidebar. -->
    <aside class="sticky top-0 hidden h-dvh w-[250px] shrink-0 lg:block">
      <AppSidebar />
    </aside>

    <!-- Mobile / tablet top bar with a slide-in navigation drawer. -->
    <header class="sticky top-0 z-30 flex h-14 items-center gap-2 border-b bg-sidebar px-3 lg:hidden">
      <Sheet v-model:open="mobileNavOpen">
        <SheetTrigger as-child>
          <Button variant="ghost" size="icon" aria-label="Menüyü aç">
            <MenuIcon />
          </Button>
        </SheetTrigger>
        <SheetContent side="left" class="w-[280px] gap-0 border-0 bg-sidebar p-0 sm:max-w-[280px]">
          <SheetTitle class="sr-only">Menü</SheetTitle>
          <SheetDescription class="sr-only">Harbour sayfaları arasında gezinin.</SheetDescription>
          <AppSidebar />
        </SheetContent>
      </Sheet>
      <BrandMark compact />
      <ThemeToggle icon-only class="ml-auto" />
    </header>

    <!-- Main content sits directly on the page background. -->
    <main id="main" class="min-w-0 flex-1">
      <div class="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 lg:px-[30px] lg:pt-[26px] lg:pb-7">
        <RouterView />
      </div>
    </main>

    <!-- One shared New content dialog and Edit sheet; pages open them via useContentDialog(). -->
    <NewContentDialog />
    <ContentEditSheet />
    <!-- Likewise one New brand dialog, opened via useClientDialog(). -->
    <NewClientDialog />
  </div>
</template>
