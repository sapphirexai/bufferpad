<template>
  <div class="settings-page">
    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="PLC">
        <el-select v-model="query.plcId" size="small" clearable placeholder="请选择PLC" style="width: 180px">
          <el-option v-for="item in plcOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="扫码器">
        <el-select v-model="query.scannerId" size="small" clearable placeholder="请选择扫码器" style="width: 200px">
          <el-option v-for="item in scannerOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="query.type" size="small" clearable placeholder="请选择类型" style="width: 220px">
          <el-option v-for="item in plcAddrTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
        </el-select>
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
      <el-table-column prop="plcName" label="PLC"></el-table-column>
      <el-table-column prop="scannerName" label="扫码器"></el-table-column>
      <el-table-column prop="installPositionName" label="安装位置" width="130"></el-table-column>
      <el-table-column prop="typeName" label="操作类型" width="220"></el-table-column>
      <el-table-column prop="addr" label="PLC寄存器地址"></el-table-column>
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

    <el-dialog title="PLC地址" :visible.sync="dialogVisible" width="560px" @close="closeDialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="PLC" prop="plcId">
          <el-select v-model="form.plcId" placeholder="请选择PLC" style="width: 100%">
            <el-option v-for="item in plcOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="扫码器" prop="scannerId">
          <el-select v-model="form.scannerId" placeholder="请选择扫码器" style="width: 100%">
            <el-option v-for="item in scannerOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择操作类型" style="width: 100%">
            <el-option v-for="item in plcAddrTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="PLC寄存器地址" prop="addr">
          <el-input v-model="form.addr" :placeholder="addressPlaceholder"></el-input>
          <div v-if="addressHint" class="field-tip">{{ addressHint }}</div>
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
  getPLCAddrPage,
  savePLCAddr,
  deletePLCAddr
} from '../../../modules/settings/api/plc-addr.api';
import { getPLCAddrTypeOptions, getPLCDeviceOptions, getScannerDeviceOptions } from '../../../modules/options/api';
import { isSuccessResponse, responseMessage } from '../../../shared/request/request';
import { createPageListMixin } from '../../../shared/mixins/page-list';
import { centerCellStyle } from '../../../shared/utils/format';
import { plcAddressHint, plcAddressPlaceholder } from '../../../modules/settings/models/device-type';

export default {
  name: 'PLCAddresses',
  mixins: [createPageListMixin({ pageSize: 10, pageSizes: [10, 20, 50] })],
  data() {
    return {
      query: { plcId: null, scannerId: null, type: null },
      dialogVisible: false,
      plcOptions: [],
      scannerOptions: [],
      plcAddrTypeOptions: [],
      form: this.emptyForm(),
      rules: {
        plcId: [{ required: true, message: '请选择PLC', trigger: 'change' }],
        scannerId: [{ required: true, message: '请选择扫码器', trigger: 'change' }],
        type: [{ required: true, message: '请选择操作类型', trigger: 'change' }],
        addr: [{ required: true, message: '请输入PLC端口或地址', trigger: 'blur' }]
      }
    };
  },
  computed: {
    selectedPlcType() {
      const selected = this.plcOptions.find(item => Number(item.value) === Number(this.form.plcId));
      return selected ? selected.deviceType : null;
    },
    addressPlaceholder() {
      return plcAddressPlaceholder(this.selectedPlcType);
    },
    addressHint() {
      return plcAddressHint(this.selectedPlcType);
    }
  },
  methods: {
    emptyForm() {
      return {
        id: null,
        plcId: null,
        scannerId: null,
        type: null,
        addr: ''
      };
    },
    rowStyle() {
      return centerCellStyle();
    },
    loadOptions() {
      getPLCDeviceOptions().then(res => {
        if (isSuccessResponse(res)) this.plcOptions = res.data.data || [];
      });
      getScannerDeviceOptions().then(res => {
        if (isSuccessResponse(res)) this.scannerOptions = res.data.data || [];
      });
      getPLCAddrTypeOptions().then(res => {
        if (isSuccessResponse(res)) this.plcAddrTypeOptions = res.data.data || [];
      });
    },
    buildParams() {
      return {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        plcId: this.query.plcId,
        scannerId: this.query.scannerId,
        type: this.query.type
      };
    },
    initData() {
      this.loading = true;
      getPLCAddrPage(this.buildParams()).then(res => {
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
        plcId: row.plcId,
        scannerId: row.scannerId,
        type: row.type,
        addr: row.addr
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
        savePLCAddr(this.form).then(res => {
          if (isSuccessResponse(res)) {
            this.$message.success('保存成功');
            this.closeDialog();
            this.loadOptions();
            this.initData();
          } else {
            this.$message.error(responseMessage(res));
          }
        }).catch(this.handleMutationError);
      });
    },
    remove(row) {
      this.$confirm('确认删除该PLC地址？', '提示', { type: 'warning' }).then(() => {
        deletePLCAddr(row.id).then(res => {
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

.field-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}
</style>
