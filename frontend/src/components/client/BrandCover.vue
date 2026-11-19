<script setup lang="ts">
import { computed } from 'vue'
import type { HTMLAttributes } from 'vue'
import { brandInitials } from '@/lib/covers'
import { cn } from '@/lib/utils'

/**
 * A brand's cover photo, or (without one) a flat coral-soft tile with the brand's initials.
 * Fills its positioned parent unless `class` says otherwise.
 */
const props = defineProps<{
  name: string
  url: string | null | undefined
  class?: HTMLAttributes['class']
  /** Size and placement of the initials, e.g. 'text-4xl'. */
  initialsClass?: HTMLAttributes['class']
  /** Slow zoom when a parent with the `group` class is hovered (cards). */
  zoom?: boolean
}>()

const initials = computed(() => brandInitials(props.name))
</script>

<template>
  <img
    v-if="url"
    :src="url"
    alt=""
    loading="lazy"
    :class="cn('object-cover', zoom && 'transition-transform duration-500 group-hover:scale-[1.03]', props.class)"
  />
  <span
    v-else
    aria-hidden="true"
    :class="cn('flex items-center justify-center overflow-hidden bg-coral-soft font-extrabold tracking-[-0.02em] text-coral-text select-none', props.class)"
  >
    <span :class="cn('leading-none', initialsClass)">{{ initials }}</span>
  </span>
</template>
