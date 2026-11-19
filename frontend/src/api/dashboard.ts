import { http } from './http'
import type { DashboardResponse } from './types'

export const dashboardApi = {
  get: (signal?: AbortSignal) => http.get<DashboardResponse>('/dashboard', { signal }),
}
