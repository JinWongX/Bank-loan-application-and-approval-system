import { describe, it, expect, vi } from 'vitest'
import { getMenu } from '../../src/api/menu.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('menu API', () => {
  it('getMenu should get /menu', async () => {
    const mockRes = { data: { code: 200, data: [] } }
    request.get.mockResolvedValue(mockRes)

    const res = await getMenu()

    expect(request.get).toHaveBeenCalledWith('/menu')
    expect(res).toEqual(mockRes)
  })
})
