import { describe, it, expect, vi } from 'vitest'
import {
  getDashboard,
  getByProduct,
  getByManager,
  getByTime,
  exportStatistics,
} from '../../src/api/statistic.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('statistic API', () => {
  it('getDashboard should get /statistics/dashboard', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getDashboard()

    expect(request.get).toHaveBeenCalledWith('/statistics/dashboard')
    expect(res).toEqual(mockRes)
  })

  it('getByProduct should get /statistics/by-product', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getByProduct()

    expect(request.get).toHaveBeenCalledWith('/statistics/by-product')
    expect(res).toEqual(mockRes)
  })

  it('getByManager should get /statistics/by-manager', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getByManager()

    expect(request.get).toHaveBeenCalledWith('/statistics/by-manager')
    expect(res).toEqual(mockRes)
  })

  it('getByTime should get /statistics/by-time with params', async () => {
    const params = { groupBy: 'DAY' }
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getByTime(params)

    expect(request.get).toHaveBeenCalledWith('/statistics/by-time', { params })
    expect(res).toEqual(mockRes)
  })

  it('exportStatistics should get /statistics/export with blob response', async () => {
    const params = { type: 'excel', dimension: 'product' }
    const mockRes = { data: new Blob([]), headers: { 'content-type': 'application/octet-stream' } }
    request.get.mockResolvedValue(mockRes)

    const res = await exportStatistics(params)

    expect(request.get).toHaveBeenCalledWith('/statistics/export', { params, responseType: 'blob' })
    expect(res).toEqual(mockRes)
  })
})
