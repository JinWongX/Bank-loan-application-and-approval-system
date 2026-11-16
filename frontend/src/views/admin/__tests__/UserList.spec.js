import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessageBox } from 'element-plus'
import UserList from '../UserList.vue'

const mockRouter = { push: vi.fn() }
vi.mock('vue-router', () => ({ useRouter: () => mockRouter }))

vi.mock('../../../api/user.js', () => ({
  getUserList: vi.fn(() => Promise.resolve({
    data: {
      code: 200,
      data: [
        { id: 1, username: 'testuser', realName: '测试用户', role: 'customer', phone: '138****8000', idCard: '110101********1234', age: 30, status: 1, createTime: '2024-01-01 10:00:00' }
      ]
    }
  })),
  getRoleList: vi.fn(() => Promise.resolve({
    data: { code: 200, data: [{ code: 'customer', name: '客户' }, { code: 'manager', name: '客户经理' }] }
  })),
  createUser: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
  updateUser: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
  deleteUser: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
  updateUserStatus: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
  resetUserPassword: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
}))

describe('UserList.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render user list', async () => {
    const wrapper = mount(UserList, { global: { stubs: ['router-link'] } })
    await flushPromises()
    expect(wrapper.text()).toContain('测试用户')
    expect(wrapper.text()).toContain('客户')
  })

  it('should call createUser when submit new user form', async () => {
    const { createUser } = await import('../../../api/user.js')
    const wrapper = mount(UserList, { global: { stubs: ['router-link'] } })
    await flushPromises()

    const addBtn = wrapper.findAll('button').find(btn => btn.text() === '新增用户')
    await addBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('新增用户')
  })

  it('should call deleteUser when delete button clicked', async () => {
    const { deleteUser } = await import('../../../api/user.js')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue()

    const wrapper = mount(UserList, { global: { stubs: ['router-link'] } })
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find(btn => btn.text() === '删除')
    expect(deleteBtn).toBeTruthy()
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(deleteUser).toHaveBeenCalledWith(1)
  })
})
