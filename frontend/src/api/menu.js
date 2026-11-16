import request from './request.js'

export function getMenu() {
  return request.get('/menu')
}
