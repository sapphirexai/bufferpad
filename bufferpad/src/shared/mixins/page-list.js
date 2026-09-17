import { pageRows, pageTotal, requestErrorMessage, shouldDisplayRequestError } from '../request/request'

export function createPageListMixin(options = {}) {
  return {
    data() {
      return {
        tableData: [],
        loading: false,
        pageSizes: options.pageSizes || [10, 20, 50],
        pageSize: options.pageSize || 10,
        currentPage: 1,
        total: 0
      }
    },
    methods: {
      setPageResult(res, mapRow) {
        const rows = pageRows(res)
        this.tableData = typeof mapRow === 'function' ? rows.map(mapRow) : rows
        this.total = pageTotal(res)
      },
      handlePageError(error) {
        if (this.$message) if (shouldDisplayRequestError(error, this)) this.$message.error(requestErrorMessage(error))
      },
      handleSizeChange(val) {
        this.pageSize = val
        this.currentPage = 1
        if (typeof this.initData === 'function') this.initData()
      },
      handleCurrentChange(val) {
        this.currentPage = val
        if (typeof this.initData === 'function') this.initData()
      }
    }
  }
}
