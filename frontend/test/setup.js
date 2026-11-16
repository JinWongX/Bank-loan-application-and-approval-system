import { config } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// Mock localStorage
global.localStorage = {
  store: {},
  getItem(key) {
    return this.store[key] || null
  },
  setItem(key, value) {
    this.store[key] = String(value)
  },
  removeItem(key) {
    delete this.store[key]
  },
  clear() {
    this.store = {}
  },
}

// Mock window.location
delete window.location
window.location = { href: '' }

// Mock Element Plus icons
config.global.components = {}
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  config.global.components[key] = component
}

config.global.plugins = [ElementPlus]

// Suppress console.warn noise in tests
const originalWarn = console.warn
console.warn = (...args) => {
  if (
    typeof args[0] === 'string' &&
    (args[0].includes('[Vue warn]') || args[0].includes('ExperimentalWarning'))
  ) {
    return
  }
  originalWarn(...args)
}
