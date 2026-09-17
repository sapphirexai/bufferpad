import { http } from '../../../src/shared/request/request'
describe('download authentication handling', () => {
  const interceptor = http.interceptors.response.handlers[0].rejected
  it('handles a password-change response wrapped as a download Blob', async () => {
    const listener = jest.fn(); window.addEventListener('bufferpad:auth-error', listener)
    const error = { config: {}, response: { status: 403, data: new Blob([JSON.stringify({ reason: 'PASSWORD_CHANGE_REQUIRED', msg: '修改密码' })]) } }
    try { await interceptor(error) } catch (received) { expect(received).toBe(error) }
    expect(error.authHandled).toBe(true); expect(listener).toHaveBeenCalledTimes(1)
    window.removeEventListener('bufferpad:auth-error', listener)
  })
  it('decodes CSRF errors but never loops after the one permitted retry', async () => {
    const error = { config: { csrfRetried: true }, response: { status: 403, data: new Blob([JSON.stringify({ reason: 'CSRF_INVALID', msg: '请求校验失败' })]) } }
    try { await interceptor(error) } catch (received) { expect(received).toBe(error) }
    expect(error.response.data.reason).toBe('CSRF_INVALID'); expect(error.authHandled).toBeUndefined()
  })
})
