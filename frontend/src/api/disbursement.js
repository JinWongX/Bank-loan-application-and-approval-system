import request from './request.js'

export function getPendingApplications() {
  return request.get('/disbursements/pending')
}

export function disburse(applicationId, data) {
  return request.post(`/disbursements/${applicationId}`, data)
}

export function getDisbursement(applicationId) {
  return request.get(`/disbursements/${applicationId}`)
}
