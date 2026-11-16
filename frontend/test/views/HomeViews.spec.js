import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import CustomerHomeView from '../../src/views/customer/HomeView.vue'
import ManagerHomeView from '../../src/views/manager/HomeView.vue'
import ApproverHomeView from '../../src/views/approver/HomeView.vue'
import DisburserHomeView from '../../src/views/disburser/HomeView.vue'
import AdminHomeView from '../../src/views/admin/HomeView.vue'

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

describe('HomeViews', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const views = [
    { name: '客户', component: CustomerHomeView, role: 'customer' },
    { name: '客户经理', component: ManagerHomeView, role: 'manager' },
    { name: '审批人', component: ApproverHomeView, role: 'approver_level1' },
    { name: '放款员', component: DisburserHomeView, role: 'disbursement_officer' },
    { name: '管理员', component: AdminHomeView, role: 'admin' },
  ]

  for (const { name, component } of views) {
    it(`should render ${name} home view`, () => {
      const wrapper = mount(component)
      expect(wrapper.find('h1').text()).toContain(name)
      expect(wrapper.text()).toContain('张三')
    })
  }

  it('should logout and redirect on click', async () => {
    const wrapper = mount(CustomerHomeView)
    await wrapper.find('button').trigger('click')

    expect(logoutMock).toHaveBeenCalled()
    expect(pushMock).toHaveBeenCalledWith('/login')
  })
})
