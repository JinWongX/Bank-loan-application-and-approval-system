import { describe, it, expect, vi } from 'vitest'
import {
  getProductList,
  getProductDetail,
  createProduct,
  updateProduct,
  updateProductStatus,
} from '../../src/api/product.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
  },
}))

describe('product API', () => {
  it('getProductList should get /products with params', async () => {
    const params = { status: 'ACTIVE' }
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getProductList(params)

    expect(request.get).toHaveBeenCalledWith('/products', { params })
    expect(res).toEqual(mockRes)
  })

  it('getProductDetail should get /products/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getProductDetail(7)

    expect(request.get).toHaveBeenCalledWith('/products/7')
    expect(res).toEqual(mockRes)
  })

  it('createProduct should post /products', async () => {
    const payload = { name: 'Test' }
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await createProduct(payload)

    expect(request.post).toHaveBeenCalledWith('/products', payload)
    expect(res).toEqual(mockRes)
  })

  it('updateProduct should put /products/:id', async () => {
    const payload = { name: 'Updated' }
    const mockRes = { data: { code: 200 } }
    request.put.mockResolvedValue(mockRes)

    const res = await updateProduct(7, payload)

    expect(request.put).toHaveBeenCalledWith('/products/7', payload)
    expect(res).toEqual(mockRes)
  })

  it('updateProductStatus should patch /products/:id/status', async () => {
    const mockRes = { data: { code: 200 } }
    request.patch.mockResolvedValue(mockRes)

    const res = await updateProductStatus(7, 'INACTIVE')

    expect(request.patch).toHaveBeenCalledWith('/products/7/status', { status: 'INACTIVE' })
    expect(res).toEqual(mockRes)
  })
})
