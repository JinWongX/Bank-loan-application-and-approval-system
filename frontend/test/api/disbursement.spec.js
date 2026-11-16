import { describe, it, expect, vi } from 'vitest'
import {
  getPendingApplications,
  disburse,
  getDisbursement,
} from '../../src/api/disbursement.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('disbursement API', () => {
  it('getPendingApplications should get /disbursements/pending', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getPendingApplications()

    expect(request.get).toHaveBeenCalledWith('/disbursements/pending')
    expect(res).toEqual(mockRes)
  })

  it('disburse should post /disbursements/:id', async () => {
    const payload = { amount: 10000 }
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await disburse(10, payload)

    expect(request.post).toHaveBeenCalledWith('/disbursements/10', payload)
    expect(res).toEqual(mockRes)
  })

  it('getDisbursement should get /disbursements/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getDisbursement(10)

    expect(request.get).toHaveBeenCalledWith('/disbursements/10')
    expect(res).toEqual(mockRes)
  })
})
