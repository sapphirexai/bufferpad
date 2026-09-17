<template>
  <div class="settings-page">
    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="位置名称">
        <el-input v-model="query.name" size="small" clearable placeholder="请输入位置名称"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="search">查询</el-button>
        <el-button :disabled="!$isAdmin" title="仅管理员可操作" size="small" @click="openDialog()">新增</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="tableData"
      v-loading="loading"
      border
      height="600"
      empty-text="暂无数据"
      :cell-style="rowStyle"
      :header-cell-style="rowStyle"
    >
      <el-table-column prop="id" label="ID" width="100"></el-table-column>
      <el-table-column prop="name" label="安装位置"></el-table-column>
      <el-table-column prop="sortNo" label="排序号" width="160"></el-table-column>
      <el-table-column label="操作" width="180">
        <template slot-scope="scope">
          <el-button :disabled="!$isAdmin" title="仅管理员可操作" type="text" size="mini" @click="openDialog(scope.row)">编辑</el-button>
          <el-button :disabled="!$isAdmin" title="仅管理员可操作" type="text" size="mini" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page.sync="currentPage"
      :page-sizes="pageSizes"
      :page-size.sync="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    ></el-pagination>

    <el-dialog title="设备安装位置" :visible.sync="dialogVisible" width="460px" @close="closeDialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="位置名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入位置名称"></el-input>
        </el-form-item>
        <el-form-item label="排序号" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" :step="1" style="width: 100%"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button :disabled="!$isAdmin" title="仅管理员可操作" type="primary" @click="save">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getInstallPositionPage,
  saveInstallPosition,
  deleteInstallPosition
} from '../../../modules/settings/api/install-position.api';
import { isSuccessResponse, responseMessage } from '../../../shared/request/request';
import { createPageListMixin } from '../../../shared/mixins/page-list';
import { centerCellStyle } from '../../../shared/utils/format';

export default {
  name: 'InstallPositions',
  mixins: [createPageListMixin({ pageSize: 10, pageSizes: [10, 20, 50] })],
  data() {
    return {
      query: { name: '' },
      dialogVisible: false,
      form: { id: null, name: '', sortNo: 0 },
      rules: {
        name: [{ required: true, message: '请输入位置名称', trigger: 'blur' }]
      }
    };
  },
  methods: {
    rowStyle() {
      return centerCellStyle();
    },
    buildParams() {
      return {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        name: this.query.name
      };
    },
    initData() {
      this.loading = true;
      getInstallPositionPage(this.buildParams()).then(res => {
        if (isSuccessResponse(res)) {
          this.setPageResult(res);
        } else {
          this.$message.error(responseMessage(res));
        }
      }).catch(this.handlePageError).finally(() => {
        this.loading = false;
      });
    },
    handleMutationError(error) {
      this.handlePageError(error);
    },
    search() {
      this.currentPage = 1;
      this.initData();
    },
    openDialog(row) {
      this.form = row ? { id: row.id, name: row.name, sortNo: row.sortNo } : { id: null, name: '', sortNo: 0 };
      this.dialogVisible = true;
    },
    closeDialog() {
      this.dialogVisible = false;
      if (this.$refs.formRef) this.$refs.formRef.resetFields();
    },
    save() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        saveInstallPosition(this.form).then(res => {
          if (isSuccessResponse(res)) {
            this.$message.success('保存成功');
            this.closeDialog();
            this.initData();
          } else {
            this.$message.error(responseMessage(res));
          }
        }).catch(this.handleMutationError);
      });
    },
    remove(row) {
      this.$confirm('确认删除该安装位置？', '提示', { type: 'warning' }).then(() => {
        deleteInstallPosition(row.id).then(res => {
          if (isSuccessResponse(res)) {
            this.$message.success('删除成功');
            this.initData();
          } else {
            this.$message.error(responseMessage(res));
          }
        }).catch(this.handleMutationError);
      }).catch(() => {});
    }
  },
  mounted() {
    this.initData();
  }
};
</script>

<style scoped>
.settings-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  margin-bottom: 12px;
}
</style>
