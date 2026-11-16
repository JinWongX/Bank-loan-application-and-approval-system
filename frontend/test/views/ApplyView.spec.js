import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import ApplyView from '../../src/views/customer/ApplyView.vue'
import { getProductList } from '../../src/api/product.js'
import { submitApplication } from '../../src/api/application.js'
import { uploadFiles } from '../../src/api/upload.js'

const pushMock = vi.fn()

describe('ApplyView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pushMock.mockClear()
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
        ],
      },
    })
  })

  vi.mock('vue-router', () => ({
    useRouter: () => ({ push: pushMock }),
  }))

  vi.mock('../../src/api/product.js', () => ({
    getProductList: vi.fn(),
  }))

  vi.mock('../../src/api/application.js', () => ({
    submitApplication: vi.fn(),
  }))

  vi.mock('../../src/api/upload.js', () => ({
    uploadFiles: vi.fn(),
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

  it('should render product list after loading', async () => {
    const wrapper = mount(ApplyView)
    await flushPromises()

    expect(wrapper.find('h2').text()).toBe('贷款申请')
    expect(wrapper.text()).toContain('个人信用贷')
  })

  it('should show empty when no products', async () => {
    getProductList.mockResolvedValueOnce({
      data: { code: 200, data: [] },
    })
    const wrapper = mount(ApplyView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无可申请的贷款产品')
  })

  it('should handle loadProducts error', async () => {
    getProductList.mockRejectedValueOnce(new Error('network'))
    const wrapper = mount(ApplyView)
    await flushPromises()

    expect(wrapper.vm.productList).toEqual([])
  })

  it('should open apply dialog when clicking product', async () => {
    const wrapper = mount(ApplyView)
    await flushPromises()

    await wrapper.find('.product-card').trigger('click')
    await flushPromises()

    expect(wrapper.vm.formVisible).toBe(true)
    expect(wrapper.vm.selectedProduct.name).toBe('个人信用贷')
  })

  it('should submit application when form is valid', async () => {
    submitApplication.mockResolvedValue({ data: { code: 200 } })
    const wrapper = mount(ApplyView)
    await flushPromises()

    await wrapper.find('.product-card').trigger('click')
    await flushPromises()

    wrapper.vm.form.amount = 50000
    wrapper.vm.form.termMonths = 6
    wrapper.vm.form.purpose = '装修'
    wrapper.vm.form.repaymentSource = '工资'
    wrapper.vm.uploadedMaterials.ID_CARD_FRONT = [{ fileName: 'id.jpg', filePath: '/id', materialType: 'ID_CARD_FRONT' }]
    wrapper.vm.uploadedMaterials.ID_CARD_BACK = [{ fileName: 'id.jpg', filePath: '/id2', materialType: 'ID_CARD_BACK' }]
    wrapper.vm.uploadedMaterials.INCOME_PROOF = [{ fileName: 'proof.jpg', filePath: '/proof', materialType: 'INCOME_PROOF' }]
    wrapper.vm.uploadedMaterials.BANK_STATEMENT = [{ fileName: 'statement.jpg', filePath: '/statement', materialType: 'BANK_STATEMENT' }]

    const dialogButtons = wrapper.findAll('button')
    const submitBtn = dialogButtons.find((btn) => btn.text().includes('提交申请'))
    expect(submitBtn).toBeTruthy()

    await submitBtn.trigger('click')
    await new Promise((r) => setTimeout(r, 10))

    expect(submitApplication).toHaveBeenCalled()
    expect(pushMock).toHaveBeenCalledWith('/customer/loans')
  })

  it('should show error when submit fails', async () => {
    submitApplication.mockRejectedValue({ response: { data: { msg: '提交失败' } } })
    const wrapper = mount(ApplyView)
    await flushPromises()

    await wrapper.find('.product-card').trigger('click')
    await flushPromises()

    wrapper.vm.form.amount = 50000
    wrapper.vm.form.termMonths = 6
    wrapper.vm.form.purpose = '装修'
    wrapper.vm.form.repaymentSource = '工资'
    wrapper.vm.uploadedMaterials.ID_CARD_FRONT = [{ fileName: 'id.jpg', filePath: '/id', materialType: 'ID_CARD_FRONT' }]
    wrapper.vm.uploadedMaterials.ID_CARD_BACK = [{ fileName: 'id.jpg', filePath: '/id2', materialType: 'ID_CARD_BACK' }]
    wrapper.vm.uploadedMaterials.INCOME_PROOF = [{ fileName: 'proof.jpg', filePath: '/proof', materialType: 'INCOME_PROOF' }]
    wrapper.vm.uploadedMaterials.BANK_STATEMENT = [{ fileName: 'statement.jpg', filePath: '/statement', materialType: 'BANK_STATEMENT' }]

    const dialogButtons = wrapper.findAll('button')
    const submitBtn = dialogButtons.find((btn) => btn.text().includes('提交申请'))
    await submitBtn.trigger('click')
    await new Promise((r) => setTimeout(r, 10))

    expect(ElMessage.error).toHaveBeenCalledWith('提交失败')
  })

  it('should upload file successfully', async () => {
    uploadFiles.mockResolvedValue({ data: { code: 200, data: ['/uploaded/file.jpg'] } })
    const wrapper = mount(ApplyView)
    await flushPromises()

    await wrapper.find('.product-card').trigger('click')
    await flushPromises()

    const onSuccess = vi.fn()
    const onError = vi.fn()
    const file = new File(['content'], 'id.jpg')

    await wrapper.vm.customUpload({ file, onSuccess, onError }, 'ID_CARD_FRONT')
    await flushPromises()

    expect(uploadFiles).toHaveBeenCalledWith([file])
    expect(onSuccess).toHaveBeenCalledWith('/uploaded/file.jpg')
    expect(wrapper.vm.uploadedMaterials.ID_CARD_FRONT.length).toBe(1)
  })

  it('should handle upload failure', async () => {
    uploadFiles.mockRejectedValue(new Error('upload failed'))
    const wrapper = mount(ApplyView)
    await flushPromises()

    await wrapper.find('.product-card').trigger('click')
    await flushPromises()

    const onSuccess = vi.fn()
    const onError = vi.fn()
    const file = new File(['content'], 'id.jpg')

    await wrapper.vm.customUpload({ file, onSuccess, onError }, 'ID_CARD_FRONT')
    await flushPromises()

    expect(onError).toHaveBeenCalled()
    expect(ElMessage.error).toHaveBeenCalledWith('文件上传失败')
  })

  it('should remove uploaded material', async () => {
    const wrapper = mount(ApplyView)
    await flushPromises()

    wrapper.vm.uploadedMaterials.ID_CARD_FRONT = [
      { fileName: 'id.jpg', filePath: '/id', materialType: 'ID_CARD_FRONT' },
    ]

    wrapper.vm.handleRemove({ url: '/id' }, 'ID_CARD_FRONT')

    expect(wrapper.vm.uploadedMaterials.ID_CARD_FRONT.length).toBe(0)
  })

  it('should call goHome when back button clicked', async () => {
    const wrapper = mount(ApplyView)
    await flushPromises()

    await wrapper.find('.page-header button').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/customer/home')
  })
})
