import { describe, it, expect, vi } from 'vitest'
import { login } from '../../src/api/auth.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('auth API', () => {
  it('login should post /auth/login with data', async () => {
    const payload = { username: 'admin', password: '123456' }
    const mockRes = { data: { code: 200, data: { token: 'abc' } } }
    request.post.mockResolvedValue(mockRes)

    const res = await login(payload)

    expect(request.post).toHaveBeenCalledWith('/auth/login', payload)
    expect(res).toEqual(mockRes)
  })
})
