<template>
  <main class="auth-screen"><section class="auth-card">
    <div class="brand">TOPRO / BUFFERPAD</div><h1>登录缓冲垫管理系统</h1>
    <p class="subtitle">查看产线运行状态，管理缓冲垫使用与设备配置。</p>
    <el-form ref="form" :model="form" :rules="rules" label-position="top" @submit.native.prevent="submit">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" autocomplete="username" maxlength="32" placeholder="请输入用户名" autofocus /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="current-password" maxlength="64" placeholder="请输入密码" /></el-form-item>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-button class="submit" type="primary" native-type="submit" :loading="busy">登录</el-button>
    </el-form>
    <p class="footnote">登录后保持登录状态，共用电脑使用完毕请主动退出。</p>
  </section></main>
</template>
<script>
import { login } from '../../modules/auth/api'
import { requestErrorMessage } from '../../shared/request/request'
export default {
  name: 'Login',
  data: () => ({ form: { username: '', password: '' }, busy: false, error: '', rules: {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  } }),
  methods: {
    submit() {
      if (this.busy) return
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.busy = true; this.error = ''
        try {
          const user = await login(this.form.username.trim(), this.form.password)
          this.form.password = ''
          this.$router.replace(user.mustChangePassword ? '/change-password' : '/index')
        } catch (error) { this.error = requestErrorMessage(error) }
        finally { this.busy = false }
      })
    }
  }
}
</script>
<style src="./auth.css"></style>
