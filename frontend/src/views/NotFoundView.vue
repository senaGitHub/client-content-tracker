<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <div class="flex min-h-[50vh] flex-col items-center justify-center gap-3 text-center">
    <p class="text-sm font-semibold text-coral-text">404</p>
    <h1 class="text-[30px] font-extrabold tracking-[-0.025em]">Sayfa bulunamadı</h1>
    <p class="max-w-sm text-sm text-muted-foreground">
      Aradığınız sayfa yok ya da taşınmış.
    </p>
    <Button as-child class="mt-2">
      <RouterLink to="/">Genel Bakış'a dön</RouterLink>
    </Button>
  </div>
</template>
