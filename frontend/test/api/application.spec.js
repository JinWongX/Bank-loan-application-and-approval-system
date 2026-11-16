import { describe, it, expect, vi } from 'vitest'
import {
  submitApplication,
  getMyApplications,
  getApplicationDetail,
  getApplicationFullDetail,
  addMaterials,
} from '../../src/api/application.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('application API', () => {
  it('submitApplication should post /applications', async () => {
    const payload = { productId: 1, amount: 10000 }
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await submitApplication(payload)

    expect(request.post).toHaveBeenCalledWith('/applications', payload)
    expect(res).toEqual(mockRes)
  })

  it('getMyApplications should get /applications/my', async () => {
    const mockRes = { data: { code: 200, data: [] } }
    request.get.mockResolvedValue(mockRes)

    const res = await getMyApplications()

    expect(request.get).toHaveBeenCalledWith('/applications/my')
    expect(res).toEqual(mockRes)
  })

  it('getApplicationDetail should get /applications/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getApplicationDetail(5)

    expect(request.get).toHaveBeenCalledWith('/applications/5')
    expect(res).toEqual(mockRes)
  })

  it('getApplicationFullDetail should get /applications/:id/detail', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getApplicationFullDetail(10)

    expect(request.get).toHaveBeenCalledWith('/applications/10/detail')
    expect(res).toEqual(mockRes)
  })

  it('addMaterials should post /applications/:id/materials', async () => {
    const materials = [{ fileName: 'a.pdf', filePath: '/a', materialType: 'ID_CARD' }]
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await addMaterials(3, materials)

    expect(request.post).toHaveBeenCalledWith('/applications/3/materials', materials)
    expect(res).toEqual(mockRes)
  })
})
