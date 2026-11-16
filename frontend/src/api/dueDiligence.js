import request from './request.js'

export function getPendingApplications() {
  return request.get('/due-diligence/pending')
}

export function getDueDiligenceReport(applicationId) {
  return request.get(`/due-diligence/${applicationId}`)
}

export function submitDueDiligence(applicationId, data) {
  return request.post(`/due-diligence/${applicationId}`, data)
}
