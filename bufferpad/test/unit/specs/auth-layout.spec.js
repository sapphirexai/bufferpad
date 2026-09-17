import Vue from 'vue'
import App from '../../../src/App.vue'
import { setUser, clearAuth } from '../../../src/modules/auth/state'
import { checkSession } from '../../../src/modules/auth/api'
jest.mock('../../../src/modules/auth/api', () => ({ checkSession: jest.fn() }))

describe('protected layout lifecycle', () => {
  let vm
  afterEach(() => { if (vm) vm.$destroy(); clearAuth() })
  function mount(route) {
    const created = jest.fn()
    const TestApp = Vue.extend({
      extends: App,
      components: { 'router-view': { created, render: h => h('div', 'business data') } },
      beforeCreate() {
        this.$route = route
        this.$router = { replace: jest.fn() }
      }
    })
    vm = new TestApp().$mount()
    return created
  }
  it('does not remount business requests between logout and the router redirect', async () => {
    setUser({ id: 1, role: 'ADMIN' })
    const created = mount({ path: '/index' })
    expect(vm.$el.textContent).toBe('business data')
    clearAuth()
    await Vue.nextTick()
    expect(vm.$el.textContent).toBe('')
    expect(created.mock.calls.length).toBe(1)
  })
  it('removes and redirects an administrator page when session verification finds a reader', async () => {
    setUser({ id: 1, role: 'ADMIN' })
    mount({ path: '/settings/users', matched: [{ meta: { adminOnly: true } }] })
    const user = { id: 2, role: 'USER' }
    checkSession.mockImplementationOnce(() => { setUser(user); return Promise.resolve(user) })
    await vm.verifySession()
    await Vue.nextTick()
    expect(vm.$el.textContent).toBe('')
    expect(vm.$router.replace).toHaveBeenCalledWith('/index')
  })
})
