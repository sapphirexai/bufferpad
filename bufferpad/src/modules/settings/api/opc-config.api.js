import { del, get, post } from '../../../shared/request/request'

export function getOpcConfigPage(params) {
  return get('/opcConfig/page', params)
}

export function saveOpcConfig(data) {
  return post('/opcConfig', data)
}

export function resetOpcConfig(id) {
  return del('/opcConfig/' + `${id}`)
}

