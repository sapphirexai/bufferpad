import { authState, setUser, clearAuth, routeDestination, isAdmin } from '../../../src/modules/auth/state'
describe('authentication and authorization state', () => {
  afterEach(() => clearAuth())
  it('requires login for business pages', () => {
    expect(routeDestination(null, { path: '/index' })).toBe('/login')
    expect(routeDestination(null, { path: '/login' })).toBe(null)
  })
  it('allows initial accounts directly into business pages', () => {
    for (const role of ['USER', 'ADMIN']) {
      const user = { id: 1, role, mustChangePassword: false }
      expect(routeDestination(user, { path: '/login' })).toBe('/index')
      expect(routeDestination(user, { path: '/index' })).toBe(null)
    }
  })
  it('requires password change after an explicit reset even for administrators', () => {
    const user = { id: 1, role: 'ADMIN', mustChangePassword: true }
    expect(isAdmin(user)).toBe(false)
    expect(routeDestination(user, { path: '/index' })).toBe('/change-password')
    expect(routeDestination(user, { path: '/change-password' })).toBe(null)
  })
  it('keeps the built-in administrator out of password changes while allowing user management', () => {
    const user = { id: -1, role: 'ADMIN', builtIn: true, passwordChangeAllowed: false, mustChangePassword: false }
    expect(isAdmin(user)).toBe(true)
    expect(routeDestination(user, { path: '/change-password' })).toBe('/index')
    expect(routeDestination(user, { path: '/settings/users', matched: [{ meta: { adminOnly: true } }] })).toBe(null)
  })
  it('rejects ordinary users from administrator routes', () => {
    const route = { path: '/settings/users', matched: [{ meta: { adminOnly: true } }] }
    expect(routeDestination({ role: 'USER' }, route)).toBe('/index')
    expect(routeDestination({ role: 'ADMIN' }, route)).toBe(null)
  })
  it('destroys cached layouts on account or role change, not routine verification', () => {
    setUser({ id: 1, role: 'ADMIN', mustChangePassword: false })
    const epoch = authState.epoch
    setUser({ id: 1, role: 'ADMIN', mustChangePassword: false })
    expect(authState.epoch).toBe(epoch)
    setUser({ id: 1, role: 'USER', mustChangePassword: false })
    expect(authState.epoch).toBe(epoch + 1)
    setUser({ id: 2, role: 'USER', mustChangePassword: false })
    expect(authState.epoch).toBe(epoch + 2)
  })
  it('clears account-related storage on logout and avoids remounting an anonymous login form', () => {
    localStorage.setItem('bufferPadData', 'previous-user')
    sessionStorage.setItem('locationUrl', '/index')
    clearAuth()
    const epoch = authState.epoch
    clearAuth()
    expect(authState.epoch).toBe(epoch)
    expect(localStorage.getItem('bufferPadData')).toBe(null)
    expect(sessionStorage.getItem('locationUrl')).toBe(null)
  })
})
