import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ElMessage } from 'element-plus'

const requestUseMock = vi.fn()
const responseUseMock = vi.fn()
const axiosPostMock = vi.fn()
const axiosGetMock = vi.fn()

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: requestUseMock },
        response: { use: responseUseMock },
      },
      get: axiosGetMock,
      post: axiosPostMock,
    })),
  },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
    },
  }
})

describe('request', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    localStorage.clear()
    vi.resetModules()
    await import('../../src/api/request.js')
  })

  it('should add Authorization header when token exists', () => {
    localStorage.setItem('token', 'abc')
    vi.resetModules()
    import('../../src/api/request.js')

    const requestInterceptor = requestUseMock.mock.calls[0][0]
    const config = { headers: {} }
    const result = requestInterceptor(config)
    expect(result.headers.Authorization).toBe('Bearer abc')
  })

  it('should not add Authorization header when token is absent', () => {
    const requestInterceptor = requestUseMock.mock.calls[0][0]
    const config = { headers: {} }
    const result = requestInterceptor(config)
    expect(result.headers.Authorization).toBeUndefined()
  })

  it('should handle request interceptor error', () => {
    const errorHandler = requestUseMock.mock.calls[0][1]
    expect(errorHandler(new Error('fail'))).rejects.toThrow('fail')
  })

  it('should pass response through response success handler', () => {
    const responseHandler = responseUseMock.mock.calls[0][0]
    const response = { data: 'ok' }
    expect(responseHandler(response)).toBe(response)
  })

  it('should handle 401 response error', () => {
    const responseErrorHandler = responseUseMock.mock.calls[0][1]
    const error = { response: { status: 401 } }
    expect(responseErrorHandler(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('登录已过期，请重新登录')
    expect(window.location.href).toBe('/login')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('should handle 403 response error', () => {
    const responseErrorHandler = responseUseMock.mock.calls[0][1]
    const error = { response: { status: 403 } }
    expect(responseErrorHandler(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('无权限访问')
  })

  it('should handle other response errors with msg', () => {
    const responseErrorHandler = responseUseMock.mock.calls[0][1]
    const error = { response: { status: 500, data: { msg: '服务器错误' } } }
    expect(responseErrorHandler(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('服务器错误')
  })

  it('should handle other response errors without msg', () => {
    const responseErrorHandler = responseUseMock.mock.calls[0][1]
    const error = { response: { status: 500 } }
    expect(responseErrorHandler(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('请求失败')
  })

  it('should handle network error', () => {
    const responseErrorHandler = responseUseMock.mock.calls[0][1]
    const error = new Error('network')
    expect(responseErrorHandler(error)).rejects.toEqual(error)
    expect(ElMessage.error).toHaveBeenCalledWith('网络错误')
  })
})
