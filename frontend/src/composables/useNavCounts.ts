import { computed, reactive, ref } from 'vue'
import { clientsApi, contentsApi } from '@/api'

interface NavCounts {
  /** Also used by the Brands page header. */
  brands: number
  /** The Onay Süreci badge. */
  inReview: number
}

// Module-level: the sidebar (desktop and mobile drawer) shares one copy.
const counts = ref<NavCounts | null>(null)
let inFlight: Promise<void> | null = null

/**
 * Publishes that are waiting (or being sent) in any useDeferredApprove, app-wide. They are still
 * in review on the server, but the badge should already leave them out. Every publish comes from
 * IN_REVIEW (the status rule), so subtracting them is exact.
 */
const publishing = reactive(new Set<number>())

/** Called by useDeferredApprove when a publish starts waiting (true) and when it is done (false). */
export function trackPublishing(id: number, waiting: boolean) {
  if (waiting) publishing.add(id)
  else publishing.delete(id)
}

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
  return computed<NavCounts | null>(() =>
    counts.value && { ...counts.value, inReview: Math.max(0, counts.value.inReview - publishing.size) },
  )
}
