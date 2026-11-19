import { http } from './http'
import type { ClientRequest, ClientResponse } from './types'

export const clientsApi = {
  list: (signal?: AbortSignal) => http.get<ClientResponse[]>('/clients', { signal }),
  get: (id: number, signal?: AbortSignal) => http.get<ClientResponse>(`/clients/${id}`, { signal }),
  create: (body: ClientRequest) => http.post<ClientResponse>('/clients', body),
}
