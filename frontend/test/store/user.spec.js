import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../../src/store/user.js'
import { login as loginApi } from '../../src/api/auth.js'
import { getMenu as getMenuApi } from '../../src/api/menu.js'

vi.mock('../../src/api/auth.js', () => ({
  login: vi.fn(),
}))

vi.mock('../../src/api/menu.js', () => ({
  getMenu: vi.fn(),
}))

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should initialize with empty state when no localStorage', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.menuList).toEqual([])
    expect(store.isLoggedIn).toBe(false)
  })

  it('setToken should update token and localStorage', () => {
    const store = useUserStore()
    store.setToken('token123')
    expect(store.token).toBe('token123')
    expect(localStorage.getItem('token')).toBe('token123')
    expect(store.isLoggedIn).toBe(true)
  })

  it('setUserInfo should update userInfo and localStorage', () => {
    const store = useUserStore()
    const info = { username: 'admin', role: 'admin' }
    store.setUserInfo(info)
    expect(store.userInfo).toEqual(info)
    expect(JSON.parse(localStorage.getItem('userInfo'))).toEqual(info)
  })

  it('setMenuList should update menuList', () => {
    const store = useUserStore()
    store.setMenuList([{ name: 'Home' }])
    expect(store.menuList).toEqual([{ name: 'Home' }])
  })

  it('clear should reset state and localStorage', () => {
    const store = useUserStore()
    store.setToken('token123')
    store.setUserInfo({ username: 'admin' })
    store.setMenuList([{ name: 'Home' }])

    store.clear()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.menuList).toEqual([])
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('userInfo')).toBeNull()
  })

  it('logout should clear state', () => {
    const store = useUserStore()
    store.setToken('token123')
    store.logout()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
  })

  it('login should set token and userInfo on success', async () => {
    const store = useUserStore()
    const mockData = {
      token: 'abc',
      userInfo: { username: 'admin', role: 'admin' },
    }
    loginApi.mockResolvedValue({ data: { code: 200, data: mockData } })

    const result = await store.login({ username: 'admin', password: '123' })

    expect(loginApi).toHaveBeenCalledWith({ username: 'admin', password: '123' })
    expect(result).toEqual(mockData)
    expect(store.token).toBe('abc')
    expect(store.userInfo).toEqual(mockData.userInfo)
  })

  it('login should throw on non-200 code', async () => {
    const store = useUserStore()
    loginApi.mockResolvedValue({ data: { code: 400, msg: '密码错误' } })

    await expect(store.login({ username: 'admin', password: 'wrong' })).rejects.toThrow('密码错误')
  })

  it('getMenu should set menuList and return data', async () => {
    const store = useUserStore()
    const menuData = [{ name: 'Home' }]
    getMenuApi.mockResolvedValue({ data: { code: 200, data: menuData } })

    const result = await store.getMenu()

    expect(getMenuApi).toHaveBeenCalled()
    expect(store.menuList).toEqual(menuData)
    expect(result).toEqual(menuData)
  })

  it('getHomePath should return home path based on role', () => {
    const store = useUserStore()
    store.setUserInfo({ role: 'customer' })
    expect(store.getHomePath()).toBe('/customer/home')

    store.setUserInfo({ role: 'manager' })
    expect(store.getHomePath()).toBe('/manager/home')

    store.setUserInfo({ role: 'approver_level1' })
    expect(store.getHomePath()).toBe('/approver/home')

    store.setUserInfo({ role: 'admin' })
    expect(store.getHomePath()).toBe('/admin/home')
  })

  it('getHomePath should return /login when no userInfo', () => {
    const store = useUserStore()
    expect(store.getHomePath()).toBe('/login')
  })
})
