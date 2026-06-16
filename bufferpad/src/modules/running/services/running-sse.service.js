import { buildBackendUrl } from '../../../shared/request/request'
import SseClient from '../../../shared/request/sse-client'

export function getRunningSseUrl(workLine) {
  return buildBackendUrl('/sse/devicesStatus/' + `${workLine}`)
}

export function createRunningSse(workLine, onMessage, onError) {
  return new SseClient(getRunningSseUrl(workLine), onMessage, onError)
}

