import { get, post } from '../../shared/request/request'

export function getCushionByQrCode(qrCode) {
  return get('/cushion/cushions', { qrCode })
}

export function manualScan(workLine, qrCode) {
  return post('/cushion/manualCushionInfo', { workLine, qrCode })
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

export function getCushionDetails(params) {
  return get('/cushion/detailsPage', params)
}

