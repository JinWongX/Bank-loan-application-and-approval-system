import request from './request.js'

export function getProductList(params) {
  return request.get('/products', { params })
}

export function getProductDetail(id) {
  return request.get(`/products/${id}`)
}

export function createProduct(data) {
  return request.post('/products', data)
}

export function updateProduct(id, data) {
  return request.put(`/products/${id}`, data)
}

export function updateProductStatus(id, status) {
  return request.patch(`/products/${id}/status`, { status })
}

export function deleteProduct(id) {
  return request.delete(`/products/${id}`)
}
