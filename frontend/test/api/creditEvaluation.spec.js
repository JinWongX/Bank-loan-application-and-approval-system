import { describe, it, expect, vi } from 'vitest'
import { evaluateCredit, getCreditEvaluation } from '../../src/api/creditEvaluation.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('creditEvaluation API', () => {
  it('evaluateCredit should post /credit-evaluations/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await evaluateCredit(12)

    expect(request.post).toHaveBeenCalledWith('/credit-evaluations/12')
    expect(res).toEqual(mockRes)
  })

  it('getCreditEvaluation should get /credit-evaluations/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getCreditEvaluation(12)

    expect(request.get).toHaveBeenCalledWith('/credit-evaluations/12')
    expect(res).toEqual(mockRes)
  })
})
