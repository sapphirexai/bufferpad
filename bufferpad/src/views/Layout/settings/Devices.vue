<template>
  <div class="settings-page">
    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="设备类型">
        <el-select v-model="query.type" size="small" clearable placeholder="请选择设备类型" style="width: 160px">
          <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="设备名称">
        <el-input v-model="query.name" size="small" clearable placeholder="请输入设备名称"></el-input>
      </el-form-item>
      <el-form-item label="IP">
        <el-input v-model="query.ip" size="small" clearable placeholder="请输入IP"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="search">查询</el-button>
        <el-button size="small" @click="openDialog()">新增</el-button>
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
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="typeName" label="设备类型" width="130"></el-table-column>
      <el-table-column prop="name" label="设备名称"></el-table-column>
      <el-table-column prop="ip" label="IP"></el-table-column>
      <el-table-column prop="port" label="端口" width="100"></el-table-column>
      <el-table-column prop="workLine" label="产线" width="100"></el-table-column>
      <el-table-column prop="installPositionName" label="安装位置" width="140"></el-table-column>
      <el-table-column label="操作" width="180">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="openDialog(scope.row)">编辑</el-button>
          <el-button type="text" size="mini" @click="remove(scope.row)">删除</el-button>
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

    <el-dialog title="设备信息" :visible.sync="dialogVisible" width="560px" @close="closeDialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="设备类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择设备类型" style="width: 100%">
            <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称"></el-input>
        </el-form-item>
        <el-form-item label="IP" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入IP"></el-input>
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" :step="1" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="产线" prop="workLine">
          <el-input-number v-model="form.workLine" :min="1" :step="1" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="安装位置" prop="installSeq">
          <el-select v-model="form.installSeq" placeholder="请选择安装位置" style="width: 100%">
            <el-option v-for="item in installPositionOptions" :key="item.value" :label="item.label" :value="Number(item.value)"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getDevicePage,
  saveDevice,
  deleteDevice
} from '../../../modules/settings/api/device-info.api';
import { getDeviceTypeOptions, getInstallPositionOptions } from '../../../modules/options/api';
import { isSuccessResponse, responseMessage } from '../../../shared/request/request';
import { createPageListMixin } from '../../../shared/mixins/page-list';
import { centerCellStyle } from '../../../shared/utils/format';

export default {
  name: 'Devices',
  mixins: [createPageListMixin({ pageSize: 10, pageSizes: [10, 20, 50] })],
  data() {
    return {
      query: { type: null, name: '', ip: '' },
      dialogVisible: false,
      deviceTypeOptions: [],
      installPositionOptions: [],
      form: this.emptyForm(),
      rules: {
        type: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
        name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
        ip: [{ required: true, message: '请输入IP', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口', trigger: 'change' }],
        installSeq: [{ required: true, message: '请选择安装位置', trigger: 'change' }]
      }
    };
  },
  methods: {
    emptyForm() {
      return {
        id: null,
        type: null,
        ip: '',
        port: 1,
        name: '',
        installSeq: null,
        workLine: 1
      };
    },
    rowStyle() {
      return centerCellStyle();
    },
    loadOptions() {
      getDeviceTypeOptions().then(res => {
        if (isSuccessResponse(res)) this.deviceTypeOptions = res.data.data || [];
      });
      getInstallPositionOptions().then(res => {
        if (isSuccessResponse(res)) this.installPositionOptions = res.data.data || [];
      });
    },
    buildParams() {
      return {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        type: this.query.type,
        name: this.query.name,
        ip: this.query.ip
      };
    },
    initData() {
      this.loading = true;
      getDevicePage(this.buildParams()).then(res => {
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
      this.form = row ? {
        id: row.id,
        type: row.type,
        ip: row.ip,
        port: row.port,
        name: row.name,
        installSeq: row.installSeq,
        workLine: row.workLine || 1
      } : this.emptyForm();
      this.dialogVisible = true;
    },
    closeDialog() {
      this.dialogVisible = false;
      if (this.$refs.formRef) this.$refs.formRef.resetFields();
    },
    save() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        saveDevice(this.form).then(res => {
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
      this.$confirm('确认删除该设备？', '提示', { type: 'warning' }).then(() => {
        deleteDevice(row.id).then(res => {
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
    this.loadOptions();
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
