import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import HomeView from '../../src/views/customer/HomeView.vue'

const pushMock = vi.fn()
const logoutMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('../../src/store/user.js', () => ({
  useUserStore: () => ({
    userInfo: { realName: '张三', username: 'zhangsan', role: 'customer' },
    logout: logoutMock,
  }),
}))

describe('Customer HomeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render user info', () => {
    const wrapper = mount(HomeView)

    expect(wrapper.find('h1').text()).toBe('客户首页')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('zhangsan')
    expect(wrapper.text()).toContain('客户')
  })

  it('should logout and redirect on click', async () => {
    const wrapper = mount(HomeView)

    await wrapper.find('button').trigger('click')

    expect(logoutMock).toHaveBeenCalled()
    expect(pushMock).toHaveBeenCalledWith('/login')
  })
})
