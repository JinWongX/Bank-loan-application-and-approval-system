import { describe, it, expect, vi } from 'vitest'
import { getRepaymentSchedules } from '../../src/api/repaymentSchedule.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('repaymentSchedule API', () => {
  it('getRepaymentSchedules should get /repayment-schedules/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getRepaymentSchedules(11)

    expect(request.get).toHaveBeenCalledWith('/repayment-schedules/11')
    expect(res).toEqual(mockRes)
  })
})
