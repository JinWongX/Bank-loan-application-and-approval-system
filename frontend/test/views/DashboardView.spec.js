import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import DashboardView from '../../src/views/admin/DashboardView.vue'
import {
  getDashboard,
  getByProduct,
  getByManager,
  getByTime,
  exportStatistics,
} from '../../src/api/statistic.js'

const initMock = vi.fn()
const setOptionMock = vi.fn()
const resizeMock = vi.fn()
const createObjectURLMock = vi.fn()
const revokeObjectURLMock = vi.fn()

vi.mock('../../src/utils/echarts.js', () => ({
  default: {
    init: (el) => {
      initMock(el)
      return {
        setOption: setOptionMock,
        resize: resizeMock,
      }
    },
  },
}))

vi.mock('../../src/api/statistic.js', () => ({
  getDashboard: vi.fn(),
  getByProduct: vi.fn(),
  getByManager: vi.fn(),
  getByTime: vi.fn(),
  exportStatistics: vi.fn(),
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

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    URL.createObjectURL = createObjectURLMock
    URL.createObjectURL.mockReturnValue('blob:xxx')
    URL.revokeObjectURL = revokeObjectURLMock
    getDashboard.mockResolvedValue({
      data: {
        code: 200,
        data: {
          pendingApplicationCount: 10,
          monthApplicationCount: 50,
          monthDisbursementAmount: 1000000,
          averageApprovalDurationHours: 24,
        },
      },
    })
    getByProduct.mockResolvedValue({
      data: {
        code: 200,
        data: [{ productName: '信用贷', applicationCount: 30 }],
      },
    })
    getByManager.mockResolvedValue({
      data: {
        code: 200,
        data: [{ managerName: '经理A', applicationCount: 20 }],
      },
    })
    getByTime.mockResolvedValue({
      data: {
        code: 200,
        data: [{ period: '2024-01', applicationCount: 40 }],
      },
    })
  })

  it('should render dashboard metrics', async () => {
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.find('h2').text()).toBe('统计看板')
    expect(wrapper.text()).toContain('待办申请数')
    expect(wrapper.text()).toContain('本月申请量')
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('50')
  })

  it('should initialize charts', async () => {
    mount(DashboardView)
    await flushPromises()

    expect(initMock).toHaveBeenCalledTimes(3)
    expect(setOptionMock).toHaveBeenCalled()
  })

  it('should format money correctly', async () => {
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.vm.formatMoney(1000)).toContain('¥')
    expect(wrapper.vm.formatMoney(null)).toBe('-')
    expect(wrapper.vm.formatMoney(undefined)).toBe('-')
  })

  it('should export statistics', async () => {
    exportStatistics.mockResolvedValue({
      data: new Blob(['data']),
      headers: { 'content-type': 'application/vnd.ms-excel' },
    })

    const wrapper = mount(DashboardView)
    await flushPromises()

    await wrapper.vm.handleExport()
    await flushPromises()

    expect(exportStatistics).toHaveBeenCalled()
  })

  it('should handle export error', async () => {
    exportStatistics.mockRejectedValue(new Error('export failed'))

    const wrapper = mount(DashboardView)
    await flushPromises()

    await wrapper.vm.handleExport()
    await flushPromises()

    expect(exportStatistics).toHaveBeenCalled()
  })

  it('should switch tabs and resize charts', async () => {
    const wrapper = mount(DashboardView)
    await flushPromises()

    wrapper.vm.activeTab = 'manager'
    await flushPromises()

    expect(resizeMock).toHaveBeenCalled()
  })

  it('should handle loadDashboard error', async () => {
    getDashboard.mockRejectedValueOnce(new Error('network'))
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.vm.dashboard.pendingApplicationCount).toBe(0)
  })
})
