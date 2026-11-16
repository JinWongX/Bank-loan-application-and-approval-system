import request from './request.js'

export function getRepaymentSchedules(applicationId) {
  return request.get(`/repayment-schedules/${applicationId}`)
}
