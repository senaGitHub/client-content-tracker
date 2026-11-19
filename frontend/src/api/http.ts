/**
 * Thin wrapper around fetch: prefixes the API base URL, sends/parses JSON and turns
 * RFC 7807 ProblemDetail responses into a typed ApiError the UI can react to.
 */

const BASE_URL = `${(import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')}/api`

/** Shape of the ProblemDetail bodies produced by the backend's GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  /** 400 validation errors: field name -> message. */
  errors?: Record<string, string>
  /** 409 invalid status transition. */
  currentStatus?: string
  requestedStatus?: string
  allowed?: string[]
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  /** Field-level validation messages (empty object when there are none). */
  get fieldErrors(): Record<string, string> {
    return this.problem.errors ?? {}
  }
}

type Query = Record<string, string | number | boolean | null | undefined>

interface RequestOptions {
  query?: Query
  body?: unknown
  signal?: AbortSignal
}

function buildUrl(path: string, query?: Query): string {
  const url = `${BASE_URL}${path}`
  if (!query) return url
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    // Skip empty / off filters so `?status=` or `?overdue=false` is never sent.
    if (value !== undefined && value !== null && value !== '' && value !== false) {
      params.set(key, String(value))
    }
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method,
      headers: {
        Accept: 'application/json, application/problem+json',
        ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal: options.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    // Network failure, CORS rejection or the API being down: no HTTP status at all.
    throw new ApiError(0, {
      title: 'Network error',
      detail: 'Could not reach the server. Check your connection and try again.',
    })
  }

  if (response.status === 204) return undefined as T

  const text = await response.text()
  const data: unknown = text ? safeParse(text) : undefined

  if (!response.ok) {
    const problem: ProblemDetail =
      data && typeof data === 'object' ? (data as ProblemDetail) : { title: response.statusText }
    throw new ApiError(response.status, { status: response.status, ...problem })
  }

  return data as T
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

export const http = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'body'>) => request<T>('GET', path, options),
  post: <T>(path: string, body: unknown) => request<T>('POST', path, { body }),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, { body }),
  patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, { body }),
}
