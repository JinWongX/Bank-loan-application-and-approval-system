import { describe, it, expect, vi } from 'vitest'
import { downloadFile } from '../../src/api/file.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('file API', () => {
  it('downloadFile should get url with blob response', async () => {
    const mockRes = { data: new Blob(['data']) }
    request.get.mockResolvedValue(mockRes)

    const res = await downloadFile('/files/1')

    expect(request.get).toHaveBeenCalledWith('/files/1', { responseType: 'blob' })
    expect(res).toEqual(mockRes)
  })
})
