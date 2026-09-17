import { http, responseData, isSuccessResponse, responseMessage } from '../../shared/request/request'
import { authState, setUser, clearAuth } from './state'
function data(response) {
  if (!isSuccessResponse(response)) throw new Error(responseMessage(response))
  return responseData(response)
}
export async function login(username, password) {
  const user = data(await http.post('/auth/login', { username, password }, { silentAuth: true }))
  clearAuth(); setUser(user)
  localStorage.setItem('bufferpad:session-change', String(Date.now()))
  return user
}
let checking = null
export function checkSession() {
  if (!checking) {
    const revision = authState.revision
    const pending = http.get('/auth/me', { silentAuth: true }).then(response => {
      if (revision !== authState.revision) return authState.user
      const user = data(response); setUser(user); return user
    }).catch(error => {
      if (revision !== authState.revision) return authState.user
      if (error.response && error.response.status === 401) { clearAuth(); return null }
      throw error
    }).finally(() => { if (checking === pending) checking = null })
    checking = pending
  }
  return checking
}
export function broadcastLogout() {
  clearAuth(); checking = null; localStorage.setItem('bufferpad:session-change', String(Date.now()))
}
export async function logout() { data(await http.post('/auth/logout')); broadcastLogout() }
export async function changePassword(oldPassword, newPassword) {
  data(await http.post('/auth/password', { oldPassword, newPassword })); broadcastLogout()
}
export const listUsers = () => http.get('/users').then(data)
export const createUser = user => http.post('/users', user).then(data)
export const updateUser = user => http.put('/users/' + user.id, { role: user.role, enabled: user.enabled }).then(data)
export const resetUserPassword = (id, password) => http.post('/users/' + id + '/password', { password }).then(data)
