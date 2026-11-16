import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ProductList from '../../src/views/admin/ProductList.vue'
import { getProductList, createProduct, updateProduct, updateProductStatus } from '../../src/api/product.js'

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('../../src/api/product.js', () => ({
  getProductList: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  updateProductStatus: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue(),
    },
  }
})

describe('ProductList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProductList.mockResolvedValue({
      data: {
        code: 200,
        data: [
          {
            id: 1,
            name: '个人信用贷',
            type: '信用',
            minAmount: 10000,
            maxAmount: 100000,
            interestRate: 0.05,
            minTerm: 6,
            maxTerm: 12,
            repaymentMethod: '等额本息',
            status: 'ACTIVE',
          },
          {
            id: 2,
            name: '企业贷',
            type: '经营',
            minAmount: 50000,
            maxAmount: 500000,
            interestRate: 0.06,
            minTerm: 12,
            maxTerm: 24,
            repaymentMethod: '等额本金',
            status: 'INACTIVE',
          },
        ],
      },
    })
  })

  it('should render product list', async () => {
    const wrapper = mount(ProductList)
    await flushPromises()

    expect(wrapper.find('h2').text()).toBe('产品配置')
    expect(wrapper.text()).toContain('个人信用贷')
    expect(wrapper.text()).toContain('企业贷')
  })

  it('should switch status from active to inactive', async () => {
    updateProductStatus.mockResolvedValue({ data: { code: 200 } })
    const wrapper = mount(ProductList)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const statusButton = buttons.find((btn) => btn.text().includes('停用'))
    expect(statusButton).toBeTruthy()

    await statusButton.trigger('click')
    await flushPromises()

    expect(updateProductStatus).toHaveBeenCalledWith(1, 'INACTIVE')
  })

  it('should switch status from inactive to active', async () => {
    updateProductStatus.mockResolvedValue({ data: { code: 200 } })
    const wrapper = mount(ProductList)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const statusButton = buttons.find((btn) => btn.text().includes('启用'))
    expect(statusButton).toBeTruthy()

    await statusButton.trigger('click')
    await flushPromises()

    expect(updateProductStatus).toHaveBeenCalledWith(2, 'ACTIVE')
  })

  it('should go home when back button clicked', async () => {
    const wrapper = mount(ProductList)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const backButton = buttons.find((btn) => btn.text().includes('返回首页'))
    await backButton.trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/admin/home')
  })

  it('should open add dialog and create product', async () => {
    createProduct.mockResolvedValue({ data: { code: 200 } })
    getProductList.mockResolvedValueOnce({
      data: { code: 200, data: [] },
    })

    const wrapper = mount(ProductList)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const addButton = buttons.find((btn) => btn.text().includes('新增产品'))
    await addButton.trigger('click')
    await flushPromises()

    expect(wrapper.vm.isEdit).toBe(false)
    expect(wrapper.vm.dialogVisible).toBe(true)

    wrapper.vm.form.name = '测试产品'
    wrapper.vm.form.type = '信用'
    wrapper.vm.form.repaymentMethod = '等额本息'

    const submitButton = wrapper.findAll('button').find((btn) => btn.text().includes('确定'))
    await submitButton.trigger('click')
    await new Promise((r) => setTimeout(r, 10))

    expect(createProduct).toHaveBeenCalled()
  })

  it('should open edit dialog and update product', async () => {
    updateProduct.mockResolvedValue({ data: { code: 200 } })

    const wrapper = mount(ProductList)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const editButton = buttons.find((btn) => btn.text().includes('编辑'))
    await editButton.trigger('click')
    await flushPromises()

    expect(wrapper.vm.isEdit).toBe(true)
    expect(wrapper.vm.currentId).toBe(1)

    const submitButton = wrapper.findAll('button').find((btn) => btn.text().includes('确定'))
    await submitButton.trigger('click')
    await new Promise((r) => setTimeout(r, 10))

    expect(updateProduct).toHaveBeenCalledWith(1, wrapper.vm.form)
  })

  it('should handle loadProducts error', async () => {
    getProductList.mockRejectedValueOnce(new Error('network'))

    const wrapper = mount(ProductList)
    await flushPromises()

    expect(wrapper.vm.productList).toEqual([])
  })
})
