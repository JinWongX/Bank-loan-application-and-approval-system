import request from './request.js'

export function getPendingApprovals() {
  return request.get('/approvals/pending')
}

export function getApprovalDetail(applicationId) {
  return request.get(`/approvals/${applicationId}`)
}

export function submitApproval(applicationId, data) {
  return request.post(`/approvals/${applicationId}`, data)
}
