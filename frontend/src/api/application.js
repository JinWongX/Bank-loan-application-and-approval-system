import request from './request.js'

export function submitApplication(data) {
  return request.post('/applications', data)
}

export function getMyApplications() {
  return request.get('/applications/my')
}

export function getApplicationDetail(id) {
  return request.get(`/applications/${id}`)
}

export function getApplicationFullDetail(id) {
  return request.get(`/applications/${id}/detail`)
}

export function addMaterials(id, materials) {
  return request.post(`/applications/${id}/materials`, materials)
}
