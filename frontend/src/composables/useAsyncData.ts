import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import type { Ref } from 'vue'
import { toast } from 'vue-sonner'
import { ApiError } from '@/api'

interface Options {
  /** Load on mount (default true). */
  immediate?: boolean
  /** When set, a failed load also shows an error toast with this title. */
  errorToast?: string
}

interface AsyncData<T> {
  data: Ref<T | undefined>
  error: Ref<string | null>
  loading: Ref<boolean>
  reload: () => Promise<void>
}

function isAbort(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError'
}

/**
 * Loading / error / retry state for one request. A new load (or unmounting) aborts the previous
 * one, so a slow earlier response can never overwrite a newer one.
 */
export function useAsyncData<T>(
  fetcher: (signal: AbortSignal) => Promise<T>,
  options: Options = {},
): AsyncData<T> {
  const data = shallowRef<T>()
  const error = ref<string | null>(null)
  const loading = ref(false)
  let controller: AbortController | null = null

  async function reload() {
    controller?.abort()
    const current = new AbortController()
    controller = current
    loading.value = true
    error.value = null
    try {
      data.value = await fetcher(current.signal)
    } catch (e) {
      if (isAbort(e)) return
      error.value = e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
      if (options.errorToast) toast.error(options.errorToast, { description: error.value })
    } finally {
      // Only the latest request may end the loading state.
      if (controller === current) loading.value = false
    }
  }

  if (options.immediate !== false) {
    loading.value = true
    onMounted(reload)
  }
  onBeforeUnmount(() => controller?.abort())

  return { data, error, loading, reload }
}
