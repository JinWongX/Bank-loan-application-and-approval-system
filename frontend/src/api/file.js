import request from './request.js'

export function downloadFile(url) {
  return request.get(url, {
    responseType: 'blob',
  })
}
