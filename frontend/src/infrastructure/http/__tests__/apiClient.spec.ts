import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetchData } from '@/infrastructure/http/apiClient'

describe('apiClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    import.meta.env.DEV = true
    import.meta.env.VITE_API_URL_DEV = 'http://localhost:8080/'
  })

  it('parses json responses', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        headers: new Headers({ 'Content-Type': 'application/json' }),
        json: async () => ({ id: 1 })
      })
    )

    const data = await apiFetchData<{ id: number }>('room/1', 'GET')
    expect(data.id).toBe(1)
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/room/1',
      expect.objectContaining({ method: 'GET' })
    )
  })

  it('throws on http errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        headers: new Headers()
      })
    )

    await expect(apiFetchData('room/1', 'GET')).rejects.toThrow('HTTP error! status: 500')
  })
})
