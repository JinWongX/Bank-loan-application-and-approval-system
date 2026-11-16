import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import LoginView from '../../src/views/LoginView.vue'

const pushMock = vi.fn()
const loginMock = vi.fn()
const getMenuMock = vi.fn()
const getHomePathMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('../../src/store/user.js', () => ({
  useUserStore: () => ({
    login: loginMock,
    getMenu: getMenuMock,
    getHomePath: getHomePathMock,
  }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getHomePathMock.mockReturnValue('/customer/home')
  })

  it('should render login form', () => {
    const wrapper = mount(LoginView)
    expect(wrapper.find('h2').text()).toBe('银行借贷系统登录')
    expect(wrapper.find('button').text()).toContain('登录')
  })

  it('should call login and redirect on success', async () => {
    loginMock.mockResolvedValue({})
    getMenuMock.mockResolvedValue([])
    const wrapper = mount(LoginView)

    wrapper.vm.form.username = 'admin'
    wrapper.vm.form.password = '123456'

    await wrapper.find('button').trigger('click')
    await new Promise((r) => setTimeout(r, 10))

    expect(loginMock).toHaveBeenCalled()
    expect(getMenuMock).toHaveBeenCalled()
    expect(pushMock).toHaveBeenCalledWith('/customer/home')
    expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
  })

  it('should show error message on login failure', async () => {
    loginMock.mockRejectedValue(new Error('密码错误'))
    const wrapper = mount(LoginView)

    wrapper.vm.form.username = 'admin'
    wrapper.vm.form.password = 'wrong'

    await wrapper.find('button').trigger('click')
    await new Promise((r) => setTimeout(r, 10))

    expect(ElMessage.error).toHaveBeenCalledWith('密码错误')
  })
})
