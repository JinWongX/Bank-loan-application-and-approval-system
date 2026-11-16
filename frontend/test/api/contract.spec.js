import { describe, it, expect, vi } from 'vitest'
import {
  generateContract,
  getContract,
  signContract,
  getMyContracts,
} from '../../src/api/contract.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('contract API', () => {
  it('generateContract should post /contracts/generate/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await generateContract(9)

    expect(request.post).toHaveBeenCalledWith('/contracts/generate/9')
    expect(res).toEqual(mockRes)
  })

  it('getContract should get /contracts/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getContract(9)

    expect(request.get).toHaveBeenCalledWith('/contracts/9')
    expect(res).toEqual(mockRes)
  })

  it('signContract should post /contracts/sign/:id', async () => {
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await signContract(5)

    expect(request.post).toHaveBeenCalledWith('/contracts/sign/5')
    expect(res).toEqual(mockRes)
  })

  it('getMyContracts should get /contracts/my/list', async () => {
    const mockRes = { data: { code: 200 } }
    request.get.mockResolvedValue(mockRes)

    const res = await getMyContracts()

    expect(request.get).toHaveBeenCalledWith('/contracts/my/list')
    expect(res).toEqual(mockRes)
  })
})
