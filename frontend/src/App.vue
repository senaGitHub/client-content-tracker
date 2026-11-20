<script setup lang="ts">
import { ConfigProvider } from 'reka-ui'
import { Toaster } from '@/components/ui/sonner'
</script>

<template>
  <!--
    html reserves the scrollbar gutter (main.css), so reka-ui's scroll lock must not add its own
    padding-right compensation; otherwise opening a dialog, sheet or menu shifts the page left.
  -->
  <ConfigProvider :scroll-body="false">
    <RouterView />
    <!-- Toasts are dark in both themes, so the toaster always uses sonner's dark base. -->
    <Toaster theme="dark" position="bottom-right" :visible-toasts="4" />
  </ConfigProvider>
</template>
