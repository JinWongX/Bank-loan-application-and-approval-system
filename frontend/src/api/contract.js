import request from './request.js'

export function generateContract(applicationId) {
  return request.post(`/contracts/generate/${applicationId}`)
}

export function getContract(applicationId) {
  return request.get(`/contracts/${applicationId}`)
}

export function signContract(contractId) {
  return request.post(`/contracts/sign/${contractId}`)
}

export function getMyContracts() {
  return request.get('/contracts/my/list')
}
