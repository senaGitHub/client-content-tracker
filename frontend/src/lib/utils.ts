import { type ClassValue, clsx } from 'clsx'
import { extendTailwindMerge } from 'tailwind-merge'

/**
 * Custom type-scale tokens. Keep this list in sync with the `--text-*` tokens in the
 * `@theme` block of `src/assets/main.css`.
 */
const TYPE_SCALE = ['page-title', 'section-title', 'nav', 'logo', 'kpi']

// Out of the box tailwind-merge reads an unknown `text-*` class as a colour, so `text-nav` and
// `text-foreground` would look like a conflict and `text-nav` would be dropped. Registering the
// tokens as font sizes keeps them alongside colour classes in every cn() call.
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: TYPE_SCALE,
    },
  },
})

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
