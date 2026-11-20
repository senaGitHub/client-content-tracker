import { readonly, ref } from 'vue'
import { clientsApi, contentsApi } from '@/api'

interface NavCounts {
  /** Also used by the Brands page header. */
  brands: number
  /** The Onay Süreci badge. Server truth: a publish still waiting to be sent counts until it is done. */
  inReview: number
}

// Module-level: the sidebar (desktop and mobile drawer) shares one copy.
const counts = ref<NavCounts | null>(null)
let inFlight: Promise<void> | null = null

/**
 * Sidebar counts from two light requests (a 1-item client page for its total, and the status
 * counts) instead of the whole dashboard. Failures are silent: the nav just shows no numbers.
 */
async function load() {
  try {
    const [clients, status] = await Promise.all([clientsApi.list({ size: 1 }), contentsApi.statusCounts()])
    counts.value = { brands: clients.totalElements, inReview: status.inReview }
  } catch {
    // Keep the last known counts.
  }
}

/** Reloads the counts; concurrent calls share one request. Call after anything that changes them. */
export function refreshNavCounts(): Promise<void> {
  inFlight ??= load().finally(() => {
    inFlight = null
  })
  return inFlight
}

export function useNavCounts() {
  if (!counts.value) void refreshNavCounts()
  return readonly(counts)
}
