import { http } from './http'
import type { ClientRequest, ClientResponse, Page, PageParams } from './types'

export const clientsApi = {
  /** Paged; sorted by name unless `sort` is given. Size defaults to 20 on the server (max 100). */
  list: (paging: PageParams = {}, signal?: AbortSignal) =>
    http.get<Page<ClientResponse>>('/clients', { query: { ...paging }, signal }),
  get: (id: number, signal?: AbortSignal) => http.get<ClientResponse>(`/clients/${id}`, { signal }),
  create: (body: ClientRequest) => http.post<ClientResponse>('/clients', body),
}
