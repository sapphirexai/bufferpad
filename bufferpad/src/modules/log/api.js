import { get } from '../../shared/request/request'

export function getScanLogs(params) {
  return get('/scanLogs', params)
}

