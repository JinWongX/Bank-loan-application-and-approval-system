import { describe, it, expect, vi } from 'vitest'
import {
  getPendingApprovals,
  getApprovalDetail,
  submitApproval,
} from '../../src/api/approval.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('approval API', () => {
  it('getPendingApprovals should get /approvals/pending', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getPendingApprovals()

    expect(request.get).toHaveBeenCalledWith('/approvals/pending')
    expect(res).toEqual(mockRes)
  })

  it('getApprovalDetail should get /approvals/:applicationId', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getApprovalDetail(12)

    expect(request.get).toHaveBeenCalledWith('/approvals/12')
    expect(res).toEqual(mockRes)
  })

  it('submitApproval should post /approvals/:applicationId', async () => {
    const payload = { action: 'APPROVE' }
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await submitApproval(12, payload)

    expect(request.post).toHaveBeenCalledWith('/approvals/12', payload)
    expect(res).toEqual(mockRes)
  })
})
