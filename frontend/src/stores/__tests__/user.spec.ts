import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/stores/user'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('sets and gets a user for a room', () => {
    const store = useUserStore()
    store.setUser(1, 'jordan')
    expect(store.getUser(1)?.username).toBe('jordan')
  })

  it('updates username for an existing room', () => {
    const store = useUserStore()
    store.setUser(1, 'jordan')
    store.setUser(1, 'alice')
    expect(store.user).toHaveLength(1)
    expect(store.getUser(1)?.username).toBe('alice')
  })

  it('removes only the matching room/user pair', () => {
    const store = useUserStore()
    store.setUser(1, 'jordan')
    store.setUser(2, 'alice')
    store.removeUser(1, 'jordan')
    expect(store.getUser(1)).toBeUndefined()
    expect(store.getUser(2)?.username).toBe('alice')
  })
})
