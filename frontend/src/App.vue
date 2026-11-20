<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { ConfigProvider } from 'reka-ui'
import { computed } from 'vue'
import { Toaster } from '@/components/ui/sonner'
import { useContentDialog } from '@/composables/useContentDialog'

/** The edit sheet's width from sm up (ContentEditSheet: sm:max-w-[500px]); below sm it is full width. */
const SHEET_WIDTH = 500
/** sonner's default desktop offset. */
const OFFSET = 24
/** sonner's --width (components/ui/sonner). */
const TOAST_WIDTH = 380

const { state } = useContentDialog()
// Only shift when the toast fits left of the sheet (sheet + 16px gap + toast + edge). Narrower
// screens, where the sheet is full width or nearly so, keep the normal offset.
const roomLeftOfSheet = useMediaQuery(`(min-width: ${SHEET_WIDTH + 16 + TOAST_WIDTH + OFFSET}px)`)

/** While the edit sheet is open, toasts move left of it so they never cover its buttons. */
const toastOffset = computed(() =>
  state.editOpen && roomLeftOfSheet.value ? { right: `${SHEET_WIDTH + 16}px`, bottom: `${OFFSET}px` } : `${OFFSET}px`,
)
</script>

<template>
  <!--
    html reserves the scrollbar gutter (main.css), so reka-ui's scroll lock must not add its own
    padding-right compensation; otherwise opening a dialog, sheet or menu shifts the page left.
  -->
  <ConfigProvider :scroll-body="false">
    <RouterView />
    <!-- Toasts are dark in both themes, so the toaster always uses sonner's dark base. -->
    <Toaster theme="dark" position="bottom-right" :offset="toastOffset" :visible-toasts="4" />
  </ConfigProvider>
</template>
