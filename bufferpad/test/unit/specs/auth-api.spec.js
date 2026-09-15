import { checkSession, broadcastLogout } from '../../../src/modules/auth/api'
import { authState, setUser } from '../../../src/modules/auth/state'
import { http } from '../../../src/shared/request/request'
jest.mock('../../../src/shared/request/request', () => ({
  http: { get: jest.fn(), post: jest.fn() },
  isSuccessResponse: res => res.data.codeSuccess,
  responseData: res => res.data.data,
  responseMessage: () => 'error'
}))
describe('session request lifecycle', () => {
  afterEach(() => broadcastLogout())
  it('does not restore an old account when an in-flight verification finishes after logout', async () => {
    let resolve
    http.get.mockReturnValueOnce(new Promise(done => { resolve = done }))
    setUser({ id: 1, role: 'ADMIN' })
    const pending = checkSession()
    broadcastLogout()
    resolve({ data: { codeSuccess: true, data: { id: 1, role: 'ADMIN' } } })
    await pending
    expect(authState.user).toBe(null)
  })
  it('does not remove a new login due to an old verification failure', async () => {
    let reject
    http.get.mockReturnValueOnce(new Promise((resolve, fail) => { reject = fail }))
    const pending = checkSession()
    broadcastLogout()
    setUser({ id: 2, role: 'USER' })
    reject({ response: { status: 401 } })
    await pending
    expect(authState.user.id).toBe(2)
  })
})
