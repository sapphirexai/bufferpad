import { get } from '../../../shared/request/request'

export function loadRecentOperationEvents(workLine, limit) {
  return get('/operationEvents/recent', {
    workLine,
    limit: limit || 20
  })
}
