import { computed, ref } from 'vue'

export type Theme = 'light' | 'dark'

// Keep in sync with the inline script in index.html.
const STORAGE_KEY = 'harbour-theme'

// Module-level state: every component shares the same theme.
// The inline script in index.html already applied the class before mount, so read it back.
const theme = ref<Theme>(
  typeof document !== 'undefined' && document.documentElement.classList.contains('dark') ? 'dark' : 'light',
)

function apply(value: Theme) {
  document.documentElement.classList.toggle('dark', value === 'dark')
}

export function useTheme() {
  function setTheme(value: Theme) {
    theme.value = value
    apply(value)
    try {
      localStorage.setItem(STORAGE_KEY, value)
    } catch {
      // Storage can be unavailable (private mode); the theme still applies for this session.
    }
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return {
    theme: computed(() => theme.value),
    isDark: computed(() => theme.value === 'dark'),
    setTheme,
    toggleTheme,
  }
}
