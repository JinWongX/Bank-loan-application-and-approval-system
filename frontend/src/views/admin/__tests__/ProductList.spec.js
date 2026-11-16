import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessageBox } from 'element-plus'
import ProductList from '../ProductList.vue'

const mockRouter = { push: vi.fn() }
vi.mock('vue-router', () => ({ useRouter: () => mockRouter }))

vi.mock('../../../api/product.js', () => ({
  getProductList: vi.fn(() => Promise.resolve({
    data: { code: 200, data: [{ id: 1, name: '测试产品', type: '信用贷', minAmount: 1000, maxAmount: 50000, interestRate: 0.05, minTerm: 1, maxTerm: 12, repaymentMethod: '等额本息', status: 'ACTIVE' }] }
  })),
  deleteProduct: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  updateProductStatus: vi.fn(() => Promise.resolve({ data: { code: 200 } })),
}))

describe('ProductList.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render product list', async () => {
    const wrapper = mount(ProductList, { global: { stubs: ['router-link'] } })
    await flushPromises()
    expect(wrapper.text()).toContain('测试产品')
    expect(wrapper.text()).toContain('信用贷')
  })

  it('should call deleteProduct when delete button clicked', async () => {
    const { deleteProduct } = await import('../../../api/product.js')
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue()

    const wrapper = mount(ProductList, { global: { stubs: ['router-link'] } })
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find(btn => btn.text() === '删除')
    expect(deleteBtn).toBeTruthy()
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(deleteProduct).toHaveBeenCalledWith(1)
  })

  it('should navigate to home when back button clicked', async () => {
    const wrapper = mount(ProductList, { global: { stubs: ['router-link'] } })
    await flushPromises()

    const backBtn = wrapper.findAll('button').find(btn => btn.text() === '返回首页')
    expect(backBtn).toBeTruthy()
    await backBtn.trigger('click')
    expect(mockRouter.push).toHaveBeenCalledWith('/admin/home')
  })
})
