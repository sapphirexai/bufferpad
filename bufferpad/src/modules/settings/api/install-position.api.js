import { del, get, post } from '../../../shared/request/request'

export function getInstallPositionPage(params) {
  return get('/deviceInstallPositions/page', params)
}

export function saveInstallPosition(data) {
  return post('/deviceInstallPositions', data)
}

export function deleteInstallPosition(id) {
  return del('/deviceInstallPositions/' + `${id}`)
}

