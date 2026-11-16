import { describe, it, expect } from 'vitest'
import echarts from '../../src/utils/echarts.js'

describe('echarts util', () => {
  it('should export echarts module', () => {
    expect(echarts).toBeDefined()
    expect(typeof echarts.init).toBe('function')
  })
})
