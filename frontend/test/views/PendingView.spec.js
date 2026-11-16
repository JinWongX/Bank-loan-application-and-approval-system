import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import PendingView from '../../src/views/manager/PendingView.vue'
import { getPendingApplications } from '../../src/api/dueDiligence.js'

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('../../src/api/dueDiligence.js', () => ({
  getPendingApplications: vi.fn(),
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

describe('PendingView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getPendingApplications.mockResolvedValue({
      data: {
        code: 200,
        data: [
          {
            id: 1,
            applicationNo: 'APP2024001',
            customerName: '张三',
            product: { name: '个人信用贷' },
            amount: 50000,
            creditGrade: 'A',
          },
        ],
      },
    })
  })

  it('should render pending list', async () => {
    const wrapper = mount(PendingView)
    await flushPromises()

    expect(wrapper.find('h2').text()).toBe('待尽调申请')
    expect(wrapper.text()).toContain('APP2024001')
    expect(wrapper.text()).toContain('张三')
  })

  it('should render all grade types', () => {
    const wrapper = mount(PendingView)
    expect(wrapper.vm.gradeType('A')).toBe('success')
    expect(wrapper.vm.gradeType('B')).toBe('primary')
    expect(wrapper.vm.gradeType('C')).toBe('warning')
    expect(wrapper.vm.gradeType('D')).toBe('danger')
    expect(wrapper.vm.gradeType('E')).toBe('info')
    expect(wrapper.vm.gradeType(null)).toBe('info')
  })

  it('should format amount correctly', () => {
    const wrapper = mount(PendingView)
    expect(wrapper.vm.formatAmount(1000)).toContain('¥')
    expect(wrapper.vm.formatAmount(null)).toBe('-')
    expect(wrapper.vm.formatAmount(undefined)).toBe('-')
  })

  it('should navigate to due diligence detail', async () => {
    const wrapper = mount(PendingView)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const dueDiligenceBtn = buttons.find((btn) => btn.text().includes('尽调'))
    await dueDiligenceBtn.trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/manager/due-diligence/1')
  })

  it('should go home when back button clicked', async () => {
    const wrapper = mount(PendingView)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const backButton = buttons.find((btn) => btn.text().includes('返回首页'))
    await backButton.trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/manager/home')
  })

  it('should handle loadPending error', async () => {
    getPendingApplications.mockRejectedValueOnce(new Error('network'))
    const wrapper = mount(PendingView)
    await flushPromises()

    expect(wrapper.vm.pendingList).toEqual([])
    expect(ElMessage.error).toHaveBeenCalledWith('获取待尽调列表失败')
  })

  it('should handle non-200 response', async () => {
    getPendingApplications.mockResolvedValueOnce({ data: { code: 500, msg: '失败' } })
    const wrapper = mount(PendingView)
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalledWith('失败')
  })
})
