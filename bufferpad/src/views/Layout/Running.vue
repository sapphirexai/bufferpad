<template>
  <div class="page">
    <div ref="lineRef">
      <el-row style="display: flex; justify-content: space-between; align-items: center;">
        <el-col :span="5" style=" display: flex; align-items: center;justify-content: space-between;">
          <p class="title" style="width: 20%;">产线:</p>
          <el-select
            v-model="ProdLine"
            placeholder="请选择"
            style="flex: 1;"
            :popper-append-to-body="false"
          >
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label + '号产线'"
              :value="item.value"
              :disabled="item.disabled"
            >
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="4" style="display:flex; align-items: center;">
          <p class="title">PLC状态:</p>
          <div class="connection-status">
            <div class="info" v-for="item in devicesMessage.filter(data => data.type === 1)" :key="item.id">
              <span class="name">{{ item.name }}</span>
              <div
                :class="{'blink': !item.status}"
                :style="{
                  width: '14px',
                  height: '14px',
                  backgroundColor:item.status ? '#67c23a' : 'red',
                  borderRadius: '14px'
                }">
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="4" style="display:flex; align-items: center; ">
          <p class="title">读码器状态:</p>
          <div class="connection-status">
            <div class="info" v-for="item in devicesMessage.filter(data => data.type === 0)" :key="item.id">
              <span class="name">{{ item.name }}</span>
              <div
                :class="{'blink': !item.status}"
                :style="{
                  width: '14px',
                  height: '14px',
                  backgroundColor:item.status ? '#67c23a' : 'red',
                  borderRadius: '14px'
                }">
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-divider></el-divider>
    </div>
    <div ref="cardRef">
      <el-row style="display: flex; align-items: center; justify-content:space-between;">
        <el-col :span="4">
          <el-card>
            <p class="title" style="text-align: center; margin-bottom: 16px;">缓冲垫编号</p>
            <el-form style="width: 100%;" :model="ruleForm" label-position="right" :rules="rules" ref="ruleForm">
              <el-form-item prop="qrCode">
                <el-input
                  v-model="ruleForm.qrCode"
                  size="small"
                  ref="inputQrCode"
                  placeholder="接收数据中..."
                  :disabled="!handle"
                  name="qrCode"
                  @keydown.enter.native="handleEnterKey($event)"
                ></el-input>
              </el-form-item>
            </el-form>
            <el-button
              type="success"
              style="width: 100%"
              size="small"
              @click="addItem(handle)"
              >{{ handle ? "确定" : "手动输入" }}</el-button
            >
          </el-card>
        </el-col>
        <el-col :span="4" style="height: 100%;">
          <el-card class="card" style="flex-direction:column;height:100%;">
            <p class="title">当前缓冲垫</p>
            <div class="cunrrentId">
              <span>{{ currentQrCode }}</span>
            </div>
            <div class="cunrrentPosition">
              <span>{{ showCodeName(currentScannerSeq) }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4" style="height: 100%;">
          <el-card class="card" style="flex-direction:column;height:100%;">
            <p class="title">使用次数</p>
            <div class="count" style="font-size: 56px; text-align: center;">
              <span :class="RemainCount === 0 ? 'error': (RemainCount > 5 ? 'success': 'warning')">{{ useCount }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4" style="height: 100%;">
          <el-card class="card" style="flex-direction:column;height: 100%;">
            <p class="title">剩余次数</p>
            <div class="count" style="font-size: 56px; text-align: center;">
              <span :class="RemainCount === 0 ? 'error': (RemainCount > 5 ? 'success': 'warning')">{{RemainCount}}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-divider></el-divider>
    </div>
    <div ref="actionRef" class="table-action">
      <div class="search-input">
        <span class="text">搜索缓冲垫：</span>
        <el-input
          size="small"
          placeholder="请输入缓冲垫编号"
          v-model="searchQrCode"
          @keydown.enter.native="handleSearchQrCode"
        >
          <i @click="handleSearchQrCode" slot="suffix" style="cursor: pointer;" class="el-input__icon el-icon-search"></i>
        </el-input>
      </div>
      <div class="table-button">
        <el-button @click="openDialog" size="small" type="primary">批量修改</el-button>
        <el-button icon="el-icon-upload2" size="small" @click="exportExcel">导出</el-button>
      </div>
    </div>
    <el-table
      :data="tableData"
      style="width: 100%"
      :row-class-name="tableRowClassName"
      v-loading="loading"
      empty-text="暂无数据"
      border
      height="600"
      element-loading-text="数据玩命加载中"
      :cell-style="rowStyle"
      :cell-class-name="isCheckCell"
      :header-cell-style="rowStyle"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="qrCode" label="缓冲垫编号" width="180"></el-table-column>
      <el-table-column prop="scannerSeq" label="缓冲垫位置" width="180">
        <template slot-scope="scope">{{ showCodeName(scope.row.scannerSeq) }}</template>
      </el-table-column>
      <el-table-column prop="openCount" label="开口数" width="180"></el-table-column>
      <el-table-column prop="createdDate" label="第一次使用时间" :formatter="formatDate"></el-table-column>
      <el-table-column prop="lastScanDate" label="最后一次使用时间" :formatter="formatDate"></el-table-column>
      <el-table-column prop="usedCount" label="当前使用次数">
        <template slot-scope="scope">
          <span :class="showColor(scope.row)">{{ scope.row.usedCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxUseCount" label="使用寿命">
        <template slot-scope="scope">
          <div class="update-maxUseCount" v-if="!scope.row.isCheck">
            <span>{{ scope.row.maxUseCount }}</span>
            <i @click="updateMaxUseCount(scope.row)" class="el-icon-edit"></i>
          </div>
          <div class="isCheck-maxUseCount" v-else>
            <el-input-number v-model="scope.row.maxUseCount" size="small" label="请输入"></el-input-number>
            <el-button class="button" type="primary" size="mini" @click="enterChangeMaxCount(scope.row)" >确定</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="pageSizes"
      :pages-size="pageSize"
      :pager-count="5"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    >
    </el-pagination>
    <el-dialog
      title="修改使用寿命"
      :visible.sync="dialogVisible"
      width="500px"
      @close="closeDialog"
    >
      <el-form :model="dialogRuleForm" label-width="150px" label-position="right" :rules="dialogRules" ref="dialogRuleFormRef">
        <el-form-item prop="maxUseCount" label="使用寿命">
          <el-input-number
            v-model="dialogRuleForm.maxUseCount"
            label="请输入使用寿命"
          ></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">取 消</el-button>
        <el-button type="primary" @click="enterBatchChangeMaxCount">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import { getPLCreadCodeStatus, postInfo, getPageInfo, qrCodeGetData, changeMaxCount, exportData } from '../../api';
import EventSourses from '../../api/eventSourse';
export default {
  name: 'Running',
  data() {
    return {
      handle: false,
      options: [
        {
          value: '1',
          label: '1'
        },
        {
          value: '2',
          label: '2'
        },
        {
          value: '3',
          label: '3'
        },
        {
          value: '4',
          label: '4'
        },
        {
          value: '5',
          label: '5'
        }
      ],
      ProdLine: '1',
      status: 'success',
      tableData: [],
      input: '',
      events: null,
      useCount: 0,
      devicesMessage: [{type: 0, status: 0}, {type: 1, status: 0}],
      loading: true,
      pageSizes: [8, 15, 20, 100, 10000],
      pageSize: 8,
      currentPage: 1,
      total: 0,
      Count: 0,
      ruleForm: {
        qrCode: ''
      },
      dialogRuleForm: {
        maxUseCount: ''
      },
      rules: {
        qrCode: [{ required: true, message: '请输入二维码', trigger: 'blur' }]
      },
      dialogRules: {
        maxUseCount: [{ required: true, message: '请输入使用寿命', trigger: 'blur' }]
      },
      searchQrCode: '',
      currentQrCode: '-',
      currentScannerSeq: '',
      dialogVisible: false,
      multipleSelection: []
    };
  },
  methods: {
    openDialog() {
      if (this.multipleSelection.length === 0) {
        this.$message.warning('请选择修改数据')
        return
      }
      this.dialogVisible = true
    },
    handleClose() {
      this.dialogVisible = false
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    showCodeName(value) {
      const name = value ? (value === 1 ? '上' : '下') : '-'
      return name
    },
    showColor(row) {
      const colorName = row.usedCount === row.maxUseCount ? 'info' : (row.maxUseCount - row.usedCount > 5 ? 'success' : 'warning')
      return colorName
    },
    updateMaxUseCount(row) {
      row.isCheck = true
    },
    isCheckCell({row, column, rowIndex, columnIndex}) {
      if (row.isCheck && column.property === 'maxUseCount') {
        return 'isCheckCell'
      } else {
        return ''
      }
    },
    tableRowClassName({ row, rowIndex }) {
      if (row.usedCount === row.maxUseCount) {
        return 'warning-row';
      } else {
        return '';
      }
    },

    handleSearchQrCode() {
      if (this.searchQrCode !== '') {
        qrCodeGetData(this.searchQrCode).then(res => {
          if (res.status === 200 && res.data.code === 0) {
            // eslint-disable-next-line no-mixed-operators
            this.tableData = res.data.data ? (res.data.data.filter(item => {
              return item.workLine === Number(this.ProdLine);
            }).map(item => {
              return {
                ...item,
                isCheck: false
              }
            })) : []

            this.total = this.tableData.length
            this.currentPage = 1
            this.pageSize = 10000
            this.loading = false
          }
        })
      } else {
        this.pageSize = 8
        this.currentPage = 1
        this.InitpageInfo()
      }
    },

    formatDate(row, column, cellValue, index) {
      // 把传过来的日期进行回炉重造一下，又创建了一个js的 Date对象，进行重新构造，转为String字符串
      // 最终返回 s 就可以了
      var s = new Date(cellValue).toLocaleString();
      return s;
    },
    addItem(flag) {
      if (flag) {
        // 调用添加数据到数据库的api
        this.$refs['ruleForm'].validate(valid => {
          if (valid) {
            postInfo(this.ProdLine, this.ruleForm.qrCode.trim()).then(res => {
              console.log('res:', res.data)
              if (res.data.codeSuccess) {
                this.Count = res.data.data.maxUseCount
                this.useCount = res.data.data.usedCount
                this.currentQrCode = res.data.data.qrCode
                this.currentScannerSeq = res.data.data.scannerSeq
                this.$message.success('扫码成功')
                // this.handle = false;

                this.currentPage = 1
                this.InitpageInfo();
              } else {
                this.$message.error(res.data.msg)
              }
            }).finally(() => {
              this.ruleForm.qrCode = ''
              this.$refs.inputQrCode.focus()
            });
          }
        });
      } else {
        this.handle = true;
      }
    },
    async subscribeAll() {
      while (true) {
        try {
          let responses = await Promise.all([
            getPLCreadCodeStatus(this.ProdLine)
          ]);
          for (const response of responses) {
            if (response.status === 502) {
              // 状态 502 是连接超时错误，
              // 连接挂起时间过长时可能会发生，
              // 远程服务器或代理会关闭它
              // 让我们重新连接
              continue;
            } else if (response.status !== 200) {
              // 一个 error —— 让我们显示它
              // showMessage(response.statusText);
              // 一秒后重新连接
              await new Promise(resolve => setTimeout(resolve, 3000));
              continue;
            } else {
              // 获取并显示消息
              let message = await response.statusText;
              // console.log(message);
              // showMessage(message);
              // 再次调用 subscribe() 以获取下一条消息
              continue;
            }
          }
        } catch (e) {
          // 处理其他异常，如网络异常等
          // console.error(e);
          await new Promise(resolve => setTimeout(resolve, 3000));
        }
      }
    },

    getPLCreadCodeStatus() {
      getPLCreadCodeStatus(this.ProdLine)
        .then(res => {
          if (res.status === 200) {
            if (res.data.codeSuccess) {
              this.devicesMessage = res.data.data
            } else {
              this.$message.error(res.data.msg)
            }
          }
        })
        .catch(error => {
          if (error.code === 'ECONNABORTED') {
            // 请求超时错误，处理方法
            this.$message.error('请求超时，请稍后再试！');
          } else if (error.message === 'Network Error') {
            // 网络错误，处理方法
            this.$message.error('网络连接异常，请检查您的网络设置！');
          } else {
            // 其他错误，处理方法
            this.$message.error('发生错误：' + error.message);
          }
        });
    },
    handleSizeChange(val) {
      this.searchQrCode = ''
      this.pageSize = val
      getPageInfo(this.currentPage, val)
        .then(res => {
          if (res.status === 200) {
            let result = res.data.data.data;
            this.tableData = result.filter(item => {
              return item.workLine === Number(this.ProdLine);
            }).map(item => {
              return {
                ...item,
                isCheck: false
              }
            });
            this.total = res.data.data.totalPage;
            this.loading = false;
          }
        })
        .catch(error => {
          if (error.code === 'ECONNABORTED') {
            // 请求超时错误，处理方法
            this.$message.error('请求超时，请稍后再试！');
          } else if (error.message === 'Network Error') {
            // 网络错误，处理方法
            this.$message.error('网络连接异常，请检查您的网络设置！');
          } else {
            // 其他错误，处理方法
            this.$message.error('发生错误：' + error.message);
          }
        });
    },
    handleCurrentChange(val) {
      this.searchQrCode = ''
      getPageInfo(val, this.pageSize)
        .then(res => {
          if (res.status === 200) {
            let result = res.data.data.data;
            this.tableData = result.filter(item => {
              return item.workLine === Number(this.ProdLine);
            }).map(item => {
              return {
                ...item,
                isCheck: false
              }
            });
            this.total = res.data.data.totalPage;
            this.loading = false;
          }
        })
        .catch(error => {
          if (error.code === 'ECONNABORTED') {
            // 请求超时错误，处理方法
            this.$message.error('请求超时，请稍后再试！');
          } else if (error.message === 'Network Error') {
            // 网络错误，处理方法
            this.$message.error('网络连接异常，请检查您的网络设置！');
          } else {
            // 其他错误，处理方法
            this.$message.error('发生错误：' + error.message);
          }
        });
    },
    InitpageInfo() {
      getPageInfo(this.currentPage, this.pageSize)
        .then(res => {
          if (res.status === 200) {
            let result = res.data.data.data;
            this.tableData = result.filter(item => {
              return item.workLine === Number(this.ProdLine);
            }).map(item => {
              return {
                ...item,
                isCheck: false
              }
            });

            this.total = res.data.data.totalPage;
            this.loading = false;
          }
        })
        .catch(error => {
          if (error.code === 'ECONNABORTED') {
            // 请求超时错误，处理方法
            this.$message.error('请求超时，请稍后再试！');
          } else if (error.message === 'Network Error') {
            // 网络错误，处理方法
            this.$message.error('网络连接异常，请检查您的网络设置！');
          } else {
            // 其他错误，处理方法
            this.$message.error('发生错误：' + error.message);
          }
        });
    },
    InitEventSourse() {
      // 目前的做法是和后端做的单向长链接，这里的接口就不放在 API 列表中处理，直接在这里作为参数传入
      const url = 'localhost:9001/sse/devicesStatus/' + this.ProdLine
      this.events = new EventSourses(
        url,
        res => {
          if (res.data.topic === 'cushionInfo') {
            if (res.data.data !== null) {
              // this.ruleForm.qrCode = res.data.data.qrCode
              this.currentQrCode = res.data.data.qrCode
              this.currentScannerSeq = res.data.data.scannerSeq
              this.useCount = res.data.data.usedCount
              this.Count = res.data.data.maxUseCount
              this.InitpageInfo();
              if (res.codeSuccess) {
                this.$message.success(res.msg)
              } else {
                this.$message.error(res.msg)
              }
            } else {
              this.currentQrCode = 'NoRead';
              this.$message.error(res.msg)
            }
          }
          if (res.data.topic === 'deviceStatus') {
            this.devicesMessage.forEach((e, index) => {
              if (res.data.data.id === e.id) {
                this.devicesMessage[index] = res.data.data
              }
            })
            console.log('deviceStatus:', this.devicesMessage)
          }
        }
      );
    },
    rowStyle() {
      return 'text-align:center';
    },
    handleEnterKey(event) {
      event.preventDefault();
    },
    async changeMaxUsedCount(data, row) {
      const params = {...data}
      const res = await changeMaxCount(params)
      if (res.data.code === 0) {
        this.$message.success('修改完成')
        if (!row) {
          this.dialogVisible = false
        } else {
          row.isCheck = false
        }
        this.InitpageInfo()
      } else {
        this.$message.error(res.data.msg)
      }
    },
    enterChangeMaxCount(row) {
      if (!row.maxUseCount) {
        this.$message.error('请填入有效数字')
      } else {
        const params = {
          maxUseCount: row.maxUseCount,
          ids: [row.id]
        }

        this.changeMaxUsedCount(params, row)
      }
    },
    enterBatchChangeMaxCount() {
      this.$refs['dialogRuleFormRef'].validate(valid => {
        if (valid) {
          const ids = this.multipleSelection.map(item => item.id)
          const params = {
            maxUseCount: this.dialogRuleForm.maxUseCount,
            ids
          }

          this.changeMaxUsedCount(params)
        }
      })
    },
    closeDialog() {
      this.$refs['dialogRuleFormRef'].resetFields()
      this.dialogVisible = false
    },
    exportExcel() {
      if (this.multipleSelection.length === 0) {
        this.$message.warning('请选择导出数据')
        return false
      }

      const ids = this.multipleSelection.map(item => item.id)
      exportData(ids).then(res => {
        if (res.status === 200) {
          const a = document.createElement('a')
          const blob = new Blob([res.data])
          const href = window.URL.createObjectURL(blob)

          a.href = href
          const fileName = res.headers['content-disposition']
          a.download = fileName.split('=')[1]
          document.body.appendChild(a)
          a.click()

          document.body.removeChild(a)
          window.URL.revokeObjectURL(href)
        }
      })
    }
  },
  watch: {
    ProdLine: {
      handler(newval, oldval) {
        this.getPLCreadCodeStatus();
        this.InitpageInfo();
        this.InitEventSourse();
      },
      immediate: true
    },
    currentQrCode(newVal, oldVal) {
      if (newVal === 'NoRead') {
        new Promise(function(resolve, reject) {
          setTimeout(function() {
            resolve();
          }, 0);
        }).then(function() {
          this.$message.warning('扫码失败，请手动输入');
        });
      }
    },
    RemainCount(newval, oldval) {
      if (newval === 0) {
        this.$message.error('扫码次数达上限')
      }
    }
  },
  computed: {
    RemainCount() {
      return this.Count - this.useCount
    }
  },
  mounted() {
    // this.subscribeAll()
    this.getPLCreadCodeStatus();
    this.InitpageInfo();
    this.InitEventSourse();
  },
  beforeDestroy() {
    this.events.close();
  }
};
</script>
<style lang="less">
.page {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.el-table .warning-row {
  background: rgb(255, 0, 0);
}

.el-table .success-row {
  background: #f0f9eb;
}
.card {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;

  .cunrrentId {
    font-size: 18px;
    margin: 26.5px 0;
    text-align: center;
  }

  .cunrrentPosition {
    font-size: 24px;
    text-align: center;
  }
}

.table-action {
  display: flex;
  justify-content: space-between;
  padding-bottom: 12px;

  .search-input {
    display: flex;
    align-items: center;
    & .text {
      width: 140px;
      font-size: 14px;
      font-weight: normal;
      color: #606266;
    }
  }
}

.card .title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  text-align: center;
}
.title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.card .code {
  font-size: 16px;
  font-weight: bold;
  margin-top: 16px;
}

.card .count {
  font-weight: bold;
  color: #67C23A;
  margin-top: 33px;

}
.error {
  color: #F56C6C;
}

.success {
  color: #67C23A;
}

.warning {
  color: #E6A23C;
}

.info {
  color: #909399;
}

.connection-status {
  max-height: 100px;
  margin-left: 16px;
  padding: 8px;
  overflow-y: scroll;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
    &:last-child {
      margin-bottom: 0px;
    }

    .name {
      display: inline-block;
      margin-right: 16px;
    }
  }
}

.isCheckCell {
  padding: 7.5px 0 !important;
}

.update-maxUseCount i{
  display: inline-block;
  margin-left: 4px;
  cursor: pointer;
}

.isCheck-maxUseCount {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;

  .button {
    margin-left: 24px;
    height: 28px;
  }
}

/deep/ el-input .el-input__inner {
  background-color: rgba(255, 255, 255, 0.247);
}

.blink {
  animation: blink 0.5s infinite steps(1);
}
@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}
</style>
