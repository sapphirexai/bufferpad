import Vue from 'vue'
import router from '../../../src/router'
import Header from '../../../src/components/Appheader.vue'
import { checkSession } from '../../../src/modules/auth/api'
import { clearAuth, setUser } from '../../../src/modules/auth/state'

jest.mock('../../../src/modules/auth/api', () => ({ checkSession: jest.fn(), logout: jest.fn() }))
jest.mock('../../../src/views/Layout', () => ({ render: h => h('div') }))
jest.mock('element-ui', () => ({ Message: { error: jest.fn(), warning: jest.fn() } }))
jest.mock('element-ui/lib/message', () => ({ error: jest.fn(), warning: jest.fn() }))
jest.mock('element-ui/lib/theme-chalk/message.css', () => ({}))
jest.mock('element-ui/lib/theme-chalk/base.css', () => ({}))

describe('direct business page entry', () => {
  afterEach(() => { clearAuth(); checkSession.mockReset() })
  async function destination(path) {
    const next = jest.fn()
    await router.beforeHooks[0](router.resolve(path).route, { path: '/' }, next)
    return next
  }
  it('redirects every business URL to login without a session', async () => {
    checkSession.mockResolvedValue(null)
    for (const path of ['/index', '/summary', '/record', '/details', '/logs', '/settings/users', '/settings/devices', '/settings/lifespan', '/settings/install-positions', '/settings/plc-addresses']) {
      expect(await destination(path)).toHaveBeenCalledWith('/login')
    }
    expect(await destination('/login')).toHaveBeenCalledWith(undefined)
  })
  it('fails closed when session verification is unavailable', async () => {
    checkSession.mockRejectedValue(new Error('offline'))
    expect(await destination('/summary')).toHaveBeenCalledWith('/login')
    expect(await destination('/login')).toHaveBeenCalledWith(undefined)
  })
  it('allows authenticated readers and administrators to enter business pages', async () => {
    for (const role of ['USER', 'ADMIN']) {
      checkSession.mockResolvedValue({ id: 1, role, mustChangePassword: false })
      expect(await destination('/index')).toHaveBeenCalledWith(undefined)
      expect(await destination('/summary')).toHaveBeenCalledWith(undefined)
      expect(await destination('/login')).toHaveBeenCalledWith('/index')
    }
  })
  it('shows a working login button when the header has no authenticated user', async () => {
    clearAuth()
    const push = jest.fn()
    const vm = new (Vue.extend({
      extends: Header,
      computed: { $isAdmin: () => false },
      beforeCreate() { Object.defineProperty(this, '$router', { value: { push } }) },
      components: {
        'el-button': { render(h) { return h('button', { on: this.$listeners }, this.$slots.default) } },
        'el-tag': { render(h) { return h('span', this.$slots.default) } }
      }
    }))().$mount()
    try {
      expect(vm.$el.textContent).toContain('登录')
      vm.$el.querySelector('button').click()
      expect(push).toHaveBeenCalledWith('/login')
      setUser({ id: 1, username: 'reader', role: 'USER' })
      await Vue.nextTick()
      expect(vm.$el.textContent).toContain('reader')
      expect(vm.$el.textContent).toContain('退出登录')
      expect(vm.$el.textContent).toContain('修改密码')
      setUser({ id: -1, username: 'superadmin', role: 'ADMIN', builtIn: true, passwordChangeAllowed: false })
      await Vue.nextTick()
      expect(vm.$el.textContent).toContain('超级管理员')
      expect(vm.$el.textContent).toContain('退出登录')
      expect(vm.$el.textContent).not.toContain('修改密码')
    } finally { vm.$destroy() }
  })
})
