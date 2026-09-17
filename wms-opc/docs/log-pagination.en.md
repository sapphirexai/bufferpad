# Log pagination

[简体中文](日志分页查询.md) · **English** · [All documentation](../../docs/README.en.md)

`GET /scanLogs` uses database pagination, default `currentPage=1&pageSize=20`. Barcode, message, type, and start/end filters remain available; either time boundary works independently. Ordering is `created_date DESC, id DESC`, stable for equal timestamps.

The response's `data` changed from an array to the project's page object:

```json
{
  "currentPage": 1,
  "pageSize": 20,
  "totalPage": 1000000,
  "data": []
}
```

Despite its name, `totalPage` means **total matching records**, not page count. Upgrade frontend and backend together. USER and ADMIN can query; anonymous requests return `401`.

The server normalizes nonpositive page numbers to 1 and page sizes to 20, with a maximum of 200, preventing negative parameters from disabling MyBatis-Plus pagination. The UI defaults to 20 and offers 20/50/100. Searching or changing page size resets to page one; stale asynchronous responses cannot overwrite newer results.

No schema change or rerun of legacy upgrade SQL is required; existing logs remain. Pagination avoids loading all matches into Java memory, but counts and unindexed filters/sorts still consume database resources. This change did not add online indexes to the large log table.

`ScanLogPaginationTest` uses the real mapper/pagination interceptor for boundaries, ordering, limits, and filters. `log-pagination.spec.js` covers paging, reset, empty results, errors, and request order. Earlier isolated MySQL million-row validation was recorded in private workspace document `日志分页查询验收.md`; it is not distributed in this repository or newly rerun by this translation.
