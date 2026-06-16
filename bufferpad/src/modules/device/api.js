import { get } from '../../shared/request/request'

export function getDeviceConnectionStatus(workLine) {
  return get('/device/deviceConnections/' + `${workLine}`)
}
