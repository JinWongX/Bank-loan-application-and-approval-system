import request from './request.js'

export function getDashboard() {
  return request.get('/statistics/dashboard')
}

export function getByProduct() {
  return request.get('/statistics/by-product')
}

export function getByManager() {
  return request.get('/statistics/by-manager')
}

export function getByTime(params) {
  return request.get('/statistics/by-time', { params })
}

export function exportStatistics(params) {
  return request.get('/statistics/export', { params, responseType: 'blob' })
}
