import { describe, it, expect, vi } from 'vitest'
import { uploadFiles } from '../../src/api/upload.js'
import request from '../../src/api/request.js'

vi.mock('../../src/api/request.js', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('upload API', () => {
  it('uploadFiles should post FormData to /upload', async () => {
    const file = new File(['content'], 'test.pdf')
    const mockRes = { data: { code: 200 } }
    request.post.mockResolvedValue(mockRes)

    const res = await uploadFiles([file])

    expect(request.post).toHaveBeenCalledWith(
      '/upload',
      expect.any(FormData),
      expect.objectContaining({
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    )
    expect(res).toEqual(mockRes)
  })
})
