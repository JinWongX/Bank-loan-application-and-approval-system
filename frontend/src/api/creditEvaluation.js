import request from './request.js'

export function evaluateCredit(applicationId) {
  return request.post(`/credit-evaluations/${applicationId}`)
}

export function getCreditEvaluation(applicationId) {
  return request.get(`/credit-evaluations/${applicationId}`)
}
