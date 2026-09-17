<template>
  <header class="app-header">
    <strong>缓冲垫管理系统</strong>
    <div v-if="user" class="account-actions">
      <span>{{ user.username }}</span>
      <el-tag size="mini" :type="$isAdmin ? 'warning' : 'info'">{{ user.builtIn ? '超级管理员' : ($isAdmin ? '管理员' : '普通用户 · 只读') }}</el-tag>
      <el-button v-if="user.passwordChangeAllowed !== false && !user.builtIn" type="text" @click="$router.push('/change-password')">修改密码</el-button>
      <el-button type="text" :loading="leaving" @click="exit">退出登录</el-button>
    </div>
    <div v-else class="account-actions">
      <el-button type="text" @click="$router.push('/login')">登录</el-button>
    </div>
  </header>
</template>
<script>
import { authState } from '../modules/auth/state'
import { logout } from '../modules/auth/api'
import { requestErrorMessage, shouldDisplayRequestError } from '../shared/request/request'
export default {
  name: 'AppHeader',
  data: () => ({ leaving: false }),
  computed: { user() { return authState.user } },
  methods: {
    async exit() {
      this.leaving = true
      try { await logout(); this.$router.replace('/login') }
      catch (error) { if (shouldDisplayRequestError(error, this)) this.$message.error(requestErrorMessage(error)) }
      finally { this.leaving = false }
    }
  }
}
</script>
<style scoped>
.app-header { position:absolute; top:0; left:0; right:0; height:50px; padding:0 20px; background:#2d3a4b; color:white; display:flex; align-items:center; justify-content:space-between; box-sizing:border-box; }
.app-header strong { font-size:18px; letter-spacing:1px; }
.account-actions { display:flex; align-items:center; gap:14px; font-size:14px; }
.account-actions .el-button { color:#e2ebf7; margin-left:0; }
</style>
