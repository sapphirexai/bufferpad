<template>
  <div class="users-page">
    <div class="users-heading"><div><h2>用户管理</h2><p>新建用户可直接登录；重置密码后，用户需先修改密码。</p></div><el-button type="primary" @click="openCreate">新增用户</el-button></div>
    <el-table :data="users" v-loading="loading" border empty-text="暂无用户">
      <el-table-column prop="username" label="用户名" />
      <el-table-column label="角色"><template slot-scope="scope"><el-tag :type="scope.row.role === 'ADMIN' ? 'warning' : 'info'">{{ scope.row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></template></el-table-column>
      <el-table-column label="状态"><template slot-scope="scope">{{ scope.row.enabled ? '启用' : '停用' }}</template></el-table-column>
      <el-table-column label="密码状态"><template slot-scope="scope">{{ scope.row.mustChangePassword ? '待修改密码' : '正常' }}</template></el-table-column>
      <el-table-column label="操作" width="250"><template slot-scope="scope"><el-button type="text" @click="openEdit(scope.row)">编辑权限</el-button><el-button type="text" @click="openReset(scope.row)">重置密码</el-button></template></el-table-column>
    </el-table>
    <el-dialog :title="mode === 'create' ? '新增用户' : (mode === 'edit' ? '编辑用户权限' : '重置密码')" :visible.sync="visible" width="450px" @closed="clearForm">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="mode !== 'create'" maxlength="32" placeholder="以小写字母开头，3–32位" /></el-form-item>
        <el-form-item v-if="mode !== 'reset'" label="角色"><el-select v-model="form.role"><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></el-form-item>
        <el-form-item v-if="mode === 'edit'" label="账户状态"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
        <el-form-item v-if="mode !== 'edit'" label="初始密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" maxlength="64" placeholder="8–64个字符" /></el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <el-alert v-if="mode !== 'create'" title="保存变更后，该用户需要重新登录。" type="info" :closable="false" />
      </el-form>
      <span slot="footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import { listUsers, createUser, updateUser, resetUserPassword, checkSession } from '../../modules/auth/api'
import { requestErrorMessage, shouldDisplayRequestError } from '../../shared/request/request'
export default {
  name: 'Users',
  data: () => ({ users: [], loading: false, saving: false, visible: false, mode: 'create', error: '', form: {}, rules: {
    username: [{ required: true, pattern: /^[a-z][a-z0-9_.-]{2,31}$/, message: '用户名须为3–32位，以小写字母开头', trigger: 'blur' }],
    password: [{ required: true, min: 8, max: 64, message: '请输入8–64个字符的密码', trigger: 'blur' }]
  } }),
  mounted() { this.refresh() },
  methods: {
    async refresh() { this.loading = true; try { this.users = await listUsers() } catch (error) { if (shouldDisplayRequestError(error, this)) this.$message.error(requestErrorMessage(error)) } finally { this.loading = false } },
    clearForm() { this.form = {}; this.error = ''; if (this.$refs.form) this.$refs.form.clearValidate() },
    openCreate() { this.mode = 'create'; this.form = { username: '', password: '', role: 'USER', enabled: true }; this.error = ''; this.visible = true },
    openEdit(user) { this.mode = 'edit'; this.form = { ...user }; this.error = ''; this.visible = true },
    openReset(user) { this.mode = 'reset'; this.form = { ...user, password: '' }; this.error = ''; this.visible = true },
    save() {
      if (this.saving) return
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.saving = true; this.error = ''
        try {
          if (this.mode === 'create') await createUser(this.form)
          else if (this.mode === 'edit') await updateUser(this.form)
          else await resetUserPassword(this.form.id, this.form.password)
          this.visible = false; this.$message.success('保存成功')
          const user = await checkSession()
          if (!user) this.$router.replace('/login')
          else await this.refresh()
        } catch (error) { this.error = requestErrorMessage(error) }
        finally { this.saving = false }
      })
    }
  }
}
</script>
<style scoped>
.users-page { padding:12px; }.users-heading { display:flex; align-items:center; justify-content:space-between; margin-bottom:24px; }.users-heading h2 { margin:0 0 8px; color:#2d3a4b; }.users-heading p { margin:0; color:#909399; font-size:14px; }.el-alert { margin-top:12px; }
</style>
