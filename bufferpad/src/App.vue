<template><div id="app"><router-view v-if="canRender" :key="epoch" /></div></template>
<script>
import { authState, clearAuth, routeDestination } from './modules/auth/state'
import { checkSession } from './modules/auth/api'
export default {
  name: 'App',
  computed: {
    epoch() { return authState.epoch },
    // Do not mount a protected page while an asynchronous redirect is pending.
    canRender() { return !routeDestination(authState.user, this.$route) }
  },
  mounted() {
    window.addEventListener('bufferpad:auth-error', this.onAuthError)
    window.addEventListener('bufferpad:verify-session', this.verifySession)
    window.addEventListener('storage', this.onStorage)
    window.addEventListener('focus', this.verifySession)
    document.addEventListener('visibilitychange', this.onVisibility)
    this.sessionTimer = setInterval(this.verifySession, 30000)
  },
  beforeDestroy() {
    clearInterval(this.sessionTimer)
    window.removeEventListener('bufferpad:auth-error', this.onAuthError)
    window.removeEventListener('bufferpad:verify-session', this.verifySession)
    window.removeEventListener('storage', this.onStorage)
    window.removeEventListener('focus', this.verifySession)
    document.removeEventListener('visibilitychange', this.onVisibility)
  },
  methods: {
    navigate(path) { if (this.$route.path !== path) this.$router.replace(path) },
    onAuthError(event) {
      if (event.detail.reason === 'PASSWORD_CHANGE_REQUIRED') { this.verifySession(); return }
      const hadUser = !!authState.user
      clearAuth()
      if (hadUser) this.$message.warning('登录已失效，请重新登录')
      this.navigate('/login')
    },
    onStorage(event) {
      if (event.key === 'bufferpad:session-change') { clearAuth(); this.verifySession() }
    },
    onVisibility() { if (!document.hidden) this.verifySession() },
    async verifySession() {
      if (document.hidden) return
      try {
        const user = await checkSession()
        const destination = routeDestination(user, this.$route)
        if (destination) this.navigate(destination)
      } catch (error) { /* Transient network loss is not logout; the server still authorizes every request. */ }
    }
  }
}
</script>
