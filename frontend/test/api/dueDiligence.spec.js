import { describe, it, expect, vi } from 'vitest'
import {
  getPendingApplications,
  getDueDiligenceReport,
  submitDueDiligence,
} from '../../src/api/dueDiligence.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('dueDiligence API', () => {
  it('getPendingApplications should get /due-diligence/pending', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getPendingApplications()

    expect(request.get).toHaveBeenCalledWith('/due-diligence/pending')
    expect(res).toEqual(mockRes)
  })

  it('getDueDiligenceReport should get /due-diligence/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getDueDiligenceReport(8)

    expect(request.get).toHaveBeenCalledWith('/due-diligence/8')
    expect(res).toEqual(mockRes)
  })

  it('submitDueDiligence should post /due-diligence/:id', async () => {
    const payload = { result: 'PASS' }
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await submitDueDiligence(8, payload)

    expect(request.post).toHaveBeenCalledWith('/due-diligence/8', payload)
    expect(res).toEqual(mockRes)
  })
})
