import { del, get, post } from '../../../shared/request/request'

export function getPLCAddrPage(params) {
  return get('/plcAddr/page', params)
}

export function savePLCAddr(data) {
  return post('/plcAddr', data)
}

export function deletePLCAddr(id) {
  return del('/plcAddr/' + `${id}`)
}

