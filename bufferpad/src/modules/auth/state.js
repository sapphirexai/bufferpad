import Vue from 'vue'

export const authState = new Vue({ data: { user: null, epoch: 0, revision: 0 } })
export function setUser(user) {
  const old = authState.user
  if (old !== user && (!old || !user || old.id !== user.id || old.role !== user.role || old.mustChangePassword !== user.mustChangePassword || old.builtIn !== user.builtIn || old.passwordChangeAllowed !== user.passwordChangeAllowed)) authState.epoch++
  authState.user = user
}
export function clearAuth() {
  authState.revision++
  setUser(null)
  localStorage.removeItem('bufferPadData')
  sessionStorage.removeItem('locationUrl')
}
export function isAdmin(user) { return !!user && user.role === 'ADMIN' && !user.mustChangePassword }
export function routeDestination(user, route) {
  if (!user) return route.path === '/login' ? null : '/login'
  if (route.path === '/change-password' && (user.builtIn || user.passwordChangeAllowed === false)) return '/index'
  if (user.mustChangePassword) return route.path === '/change-password' ? null : '/change-password'
  if (route.path === '/login') return '/index'
  if (route.matched && route.matched.some(item => item.meta && item.meta.adminOnly) && !isAdmin(user)) return '/index'
  return null
}
