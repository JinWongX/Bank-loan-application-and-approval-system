import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const useUserStoreMock = vi.fn()

vi.mock('../../src/store/user.js', () => ({
  useUserStore: () => useUserStoreMock(),
}))

describe('router', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.resetModules()
    useUserStoreMock.mockReturnValue({
      isLoggedIn: false,
      getHomePath: vi.fn().mockReturnValue('/login'),
      userInfo: null,
    })
  })

  it('should have routes exported', async () => {
    const router = (await import('../../src/router/index.js')).default
    expect(router).toBeDefined()
    expect(router.getRoutes().length).toBeGreaterThan(0)
    expect(router.getRoutes().some((r) => r.path === '/login')).toBe(true)
  }, 20000)

  it('should redirect / to /login', async () => {
    const router = (await import('../../src/router/index.js')).default
    await router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
  }, 20000)

  it('should redirect logged-in user visiting /login to home', async () => {
    useUserStoreMock.mockReturnValue({
      isLoggedIn: true,
      getHomePath: vi.fn().mockReturnValue('/customer/home'),
      userInfo: { role: 'customer' },
    })
    const router = (await import('../../src/router/index.js')).default
    await router.push('/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/customer/home')
  }, 20000)

  it('should redirect unauthenticated user from protected route to /login', async () => {
    useUserStoreMock.mockReturnValue({
      isLoggedIn: false,
      getHomePath: vi.fn().mockReturnValue('/login'),
      userInfo: null,
    })
    const router = (await import('../../src/router/index.js')).default
    await router.push('/customer/home')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
  }, 20000)

  it('should redirect user with wrong role to home', async () => {
    useUserStoreMock.mockReturnValue({
      isLoggedIn: true,
      getHomePath: vi.fn().mockReturnValue('/manager/home'),
      userInfo: { role: 'manager' },
    })
    const router = (await import('../../src/router/index.js')).default
    await router.push('/customer/home')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/manager/home')
  }, 20000)

  it('should allow access when role matches', async () => {
    useUserStoreMock.mockReturnValue({
      isLoggedIn: true,
      getHomePath: vi.fn().mockReturnValue('/customer/home'),
      userInfo: { role: 'customer' },
    })
    const router = (await import('../../src/router/index.js')).default
    await router.push('/customer/home')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/customer/home')
  }, 20000)
})
