<template>
  <span class="time-range-export">
    <el-button icon="el-icon-date" size="small" @click="open">时间段导出</el-button>
    <el-dialog title="时间段导出" :visible.sync="visible" width="580px" append-to-body
      :close-on-click-modal="false" :close-on-press-escape="!exporting" :show-close="!exporting">
      <el-form label-width="110px">
        <el-form-item label="时间类型">
          <el-select v-model="timeType" :disabled="exporting" style="width: 100%">
            <el-option label="第一次使用时间" value="FIRST_USE" />
            <el-option label="最后一次使用时间" value="LAST_USE" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="range" type="datetimerange" :disabled="exporting"
            start-placeholder="开始时间" end-placeholder="结束时间" value-format="yyyy-MM-dd HH:mm:ss"
            :default-time="['00:00:00', '23:59:59']" style="width: 100%" />
        </el-form-item>
      </el-form>
      <p class="time-export-tip">导出所选时间段内的缓冲垫当前信息，包含起止时间，不受分页、勾选或页面搜索条件影响。最多导出 100000 条。</p>
      <span slot="footer">
        <el-button :disabled="exporting" @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="submit">{{ exporting ? '正在导出' : '导出' }}</el-button>
      </span>
    </el-dialog>
  </span>
</template>
<script>
import dayjs from 'dayjs'
import { exportCushionsByTime } from '../../cushion/api'
import { downloadBlob, getFilenameFromDisposition } from '../../../shared/utils/download'
import { requestErrorMessage, shouldDisplayRequestError } from '../../../shared/request/request'

function blobText(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsText(blob)
  })
}
async function exportError(response) {
  let data = response && response.data
  if (data instanceof Blob) {
    try { data = JSON.parse(await blobText(data)) } catch (ignored) { return '导出失败，请稍后重试' }
  }
  return data && data.msg ? data.msg : '导出失败，请稍后重试'
}
export default {
  name: 'TimeRangeExport',
  data() { return { visible: false, exporting: false, timeType: 'LAST_USE', range: null } },
  methods: {
    open() {
      this.timeType = 'LAST_USE'
      this.range = [dayjs().startOf('day').format('YYYY-MM-DD HH:mm:ss'), dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')]
      this.visible = true
    },
    async submit() {
      if (this.exporting) return
      if (!this.range || this.range.length !== 2 || !this.range[0] || !this.range[1]) {
        this.$message.warning('请选择起止时间'); return
      }
      if (this.range[0] > this.range[1]) { this.$message.warning('开始时间不能晚于结束时间'); return }
      this.exporting = true
      try {
        const response = await exportCushionsByTime({ timeType: this.timeType, startTime: this.range[0], endTime: this.range[1] })
        const type = response.headers && response.headers['content-type'] || ''
        if (type.indexOf('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') < 0) {
          throw new Error(await exportError(response))
        }
        downloadBlob(response.data, getFilenameFromDisposition(response.headers['content-disposition'], '缓冲垫时间段导出.xlsx'))
        this.visible = false
        this.$message.success('导出成功')
      } catch (error) {
        if (shouldDisplayRequestError(error, this)) {
          this.$message.error(error.response ? await exportError(error.response) : requestErrorMessage(error))
        }
      } finally { this.exporting = false }
    }
  }
}
</script>
<style scoped>
.time-range-export { display: inline-block; margin-left: 10px; }
.time-export-tip { color: #606266; line-height: 1.7; margin: 0 20px; }
</style>
