<template>
  <div class="settings-page">
    <div class="toolbar">
      <el-button :disabled="!$isAdmin" title="仅管理员可操作" type="primary" size="small" @click="openDialog">编辑寿命</el-button>
    </div>
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      height="600"
      empty-text="暂无数据"
      :cell-style="rowStyle"
      :header-cell-style="rowStyle"
    >
      <el-table-column prop="id" label="ID" width="120"></el-table-column>
      <el-table-column prop="cushionMaxUseCount" label="全局缓冲垫最大使用数量"></el-table-column>
      <el-table-column label="操作" width="220">
        <template slot-scope="scope">
          <el-button :disabled="!$isAdmin" title="仅管理员可操作" type="text" size="mini" @click="openDialog(scope.row)">编辑</el-button>
          <el-button :disabled="!$isAdmin" title="仅管理员可操作" type="text" size="mini" @click="resetDefault(scope.row)">重置默认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="缓冲垫寿命设置" :visible.sync="dialogVisible" width="420px" @close="closeDialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="160px">
        <el-form-item label="最大使用数量" prop="cushionMaxUseCount">
          <el-input-number v-model="form.cushionMaxUseCount" :min="1" :step="1" style="width: 100%"></el-input-number>
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
import { getOpcConfigPage, saveOpcConfig, resetOpcConfig } from '../../../modules/settings/api/opc-config.api';
import { isSuccessResponse, pageRows, requestErrorMessage, shouldDisplayRequestError, responseMessage } from '../../../shared/request/request';
import { centerCellStyle } from '../../../shared/utils/format';

export default {
  name: 'Lifespan',
  data() {
    return {
      loading: false,
      tableData: [],
      dialogVisible: false,
      form: {
        id: 1,
        cushionMaxUseCount: 500
      },
      rules: {
        cushionMaxUseCount: [{ required: true, message: '请输入最大使用数量', trigger: 'change' }]
      }
    };
  },
  methods: {
    rowStyle() {
      return centerCellStyle();
    },
    initData() {
      this.loading = true;
      getOpcConfigPage({ currentPage: 1, pageSize: 10 }).then(res => {
        if (isSuccessResponse(res)) {
          this.tableData = pageRows(res);
        } else {
          this.$message.error(responseMessage(res));
        }
      }).catch(error => {
        if (shouldDisplayRequestError(error, this)) this.$message.error(requestErrorMessage(error));
      }).finally(() => {
        this.loading = false;
      });
    },
    openDialog(row) {
      const data = row || this.tableData[0] || { id: 1, cushionMaxUseCount: 500 };
      this.form = {
        id: 1,
        cushionMaxUseCount: data.cushionMaxUseCount
      };
      this.dialogVisible = true;
    },
    closeDialog() {
      this.dialogVisible = false;
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields();
      }
    },
    save() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        saveOpcConfig(this.form).then(res => {
          if (isSuccessResponse(res)) {
            this.$message.success('保存成功');
            this.closeDialog();
            this.initData();
          } else {
            this.$message.error(responseMessage(res));
          }
        }).catch(error => {
          if (shouldDisplayRequestError(error, this)) this.$message.error(requestErrorMessage(error));
        });
      });
    },
    resetDefault(row) {
      this.$confirm('将缓冲垫最大使用数量重置为默认值 500，是否继续？', '提示', { type: 'warning' }).then(() => {
        resetOpcConfig(row.id).then(res => {
          if (isSuccessResponse(res)) {
            this.$message.success('已重置');
            this.initData();
          } else {
            this.$message.error(responseMessage(res));
          }
        }).catch(error => {
          if (shouldDisplayRequestError(error, this)) this.$message.error(requestErrorMessage(error));
        });
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
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
