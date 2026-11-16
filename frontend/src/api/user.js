import request from './request.js'

export function getUserList() {
  return request.get('/users')
}

export function getUserDetail(id) {
  return request.get(`/users/${id}`)
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

export function updateUserStatus(id, status) {
  return request.patch(`/users/${id}/status`, { status })
}

export function resetUserPassword(id, newPassword) {
  return request.patch(`/users/${id}/reset-password`, { newPassword })
}

export function getRoleList() {
  return request.get('/roles')
}
