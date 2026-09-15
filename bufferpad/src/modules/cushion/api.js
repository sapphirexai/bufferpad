import { get, post, request } from '../../shared/request/request'

export function getCushionByQrCode(qrCode) {
  return get('/cushion/cushions', { qrCode })
}

export function manualScan(workLine, qrCode, operationId) {
  return request({
    method: 'post',
    url: '/cushion/manualCushionInfo',
    data: { workLine, qrCode },
    headers: { 'X-Operation-Id': operationId }
  })
}

export function getCushionPage(params) {
  return get('/cushion/cushionsPage', params)
}

export function updateCushionMaxUseCount(data) {
  return post('/cushion/cushions', data)
}

export function exportCushions(data) {
  return post('/cushion/cushions/excel', data, 'blob')
}

export function exportCushionsByTime(data) {
  return request({ method: 'post', url: '/cushion/cushions/excel/time-range', data, responseType: 'blob', timeout: 180000 })
}

export function getCushionDetails(params) {
  return get('/cushion/detailsPage', params)
}
