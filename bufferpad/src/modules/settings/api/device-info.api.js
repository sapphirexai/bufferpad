import { del, get, post } from '../../../shared/request/request'

export function getDevicePage(params) {
  return get('/device/devicesPage', params)
}

export function saveDevice(data) {
  return post('/device', data)
}

export function deleteDevice(id) {
  return del('/device/' + `${id}`)
}
