<template>
  <main class="auth-screen"><section class="auth-card">
    <div class="brand">BUFFERPAD / 账户设置</div><h1>修改密码</h1>
    <p class="subtitle">{{ forced ? '密码已被重置，请设置新密码后使用系统。' : '修改后，所有设备上的登录都会失效，请使用新密码重新登录。' }}</p>
    <el-form ref="form" :model="form" :rules="rules" label-position="top" @submit.native.prevent="submit">
      <el-form-item label="原密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" maxlength="64" /></el-form-item>
      <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" maxlength="64" placeholder="8–64个字符" /></el-form-item>
      <el-form-item label="确认新密码" prop="confirm"><el-input v-model="form.confirm" type="password" show-password autocomplete="new-password" maxlength="64" /></el-form-item>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-button class="submit" type="primary" native-type="submit" :loading="busy">确认修改</el-button>
      <el-button v-if="!forced" type="text" @click="$router.replace('/index')">返回系统</el-button>
      <el-button type="text" @click="exit">退出登录</el-button>
    </el-form>
  </section></main>
</template>
<script>
import { authState } from '../../modules/auth/state'
import { changePassword, logout } from '../../modules/auth/api'
import { requestErrorMessage } from '../../shared/request/request'
export default {
  name: 'ChangePassword',
  data() {
    return { form: { oldPassword: '', newPassword: '', confirm: '' }, busy: false, error: '', rules: {
      oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
      newPassword: [{ required: true, min: 8, max: 64, message: '请输入8–64个字符的新密码', trigger: 'blur' }],
      confirm: [{ validator: (rule, value, done) => done(value && value === this.form.newPassword ? undefined : new Error('两次输入的新密码不一致')), trigger: 'blur' }]
    } }
  },
  computed: { forced() { return authState.user && authState.user.mustChangePassword } },
  methods: {
    submit() {
      if (this.busy) return
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.busy = true; this.error = ''
        try { await changePassword(this.form.oldPassword, this.form.newPassword); this.$message.success('密码已修改，请重新登录'); this.$router.replace('/login') }
        catch (error) { this.error = requestErrorMessage(error) }
        finally { this.busy = false }
      })
    },
    async exit() {
      try { await logout(); this.$router.replace('/login') }
      catch (error) { this.error = requestErrorMessage(error) }
    }
  }
}
</script>
<style src="./auth.css"></style>
