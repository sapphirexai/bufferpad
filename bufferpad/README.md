# bufferpad 前端

PCB 回流线缓冲垫计数项目的 Vue 2 前端，提供运行监控、缓冲垫汇总、使用记录、
设备状态、PLC 地址及基础配置页面。运行监控通过 SSE 接收扫码、计数和 PLC 处理结果。

## 功能模块

| 菜单 | 功能 |
| --- | --- |
| 运行监控 | 产线设备摘要、扫码工作台、寿命进度、运行告警和缓冲垫分页列表 |
| 缓冲垫汇总 | 按日期统计缓冲垫使用情况 |
| 使用记录 | 查询和导出缓冲垫使用明细 |
| 设置 / 缓冲垫寿命 | 维护全局最大使用次数 |
| 设置 / 设备安装位置 | 维护现场工位名称和排序 |
| 设置 / 设备信息 | 维护扫码器、三菱 PLC、汇川 PLC、西门子 S7-1200/S7-1500 的连接参数 |
| 设置 / PLC地址 | 维护扫码器操作类型与 PLC 寄存器的映射 |

选择西门子 S7-1200 或 S7-1500 时，设备端口自动建议为 `102`；PLC 地址页会提示使用 `DB1.DBW0`、`MW0`、`IW0`、`QW0` 等 16 位绝对地址。

## 运行监控页面

运行监控按照“生产操作优先、异常信息优先”的原则组织：

- 顶部设备栏保持单行摘要，只显示“设备异常总数、PLC 在线数、读码器在线数”。异常设备
  数量统一使用“异常”口径，避免“需关注、另有、待关注”等多套数字同时出现。
- 点击“查看设备”可在右侧抽屉中查看完整设备名称、安装位置、IP、端口、状态变化时间、
  最近通信时间和错误原因，不在首页堆叠设备列表。
- 最近一次扫码或 PLC 处理结果使用单行反馈条展示，长文本自动省略；现场处理建议、二维码、
  设备、寄存器地址和错误码统一放入“运行告警与最近操作”抽屉，正文不会再压住下方内容。
- 原四张统计卡改为一条扫码工作台，将扫码输入、当前缓冲垫、已用/寿命、剩余次数和寿命进度
  集中展示。尚未扫码或扫码失败时显示“等待扫码/扫码失败”和 `--`，不再显示误导性的红色 `0`。
- 缓冲垫表格默认每页 `10` 条，缩小工具栏和行高，编号与日期保持单行并支持悬停查看完整值；
  页面使用自然纵向滚动，较窄窗口下表格使用自身横向滚动。
- 2026-08-03 视觉验收结果：`1920x1080` 首屏显示 10 行，`1366x768` 首屏显示 8 行，
  `1024x768` 首屏显示 7 行；三档分辨率均无页面横向溢出，设备和告警抽屉可完整阅读。

### 自动更新规则

- 页面进入或切换产线时，通过 HTTP 加载设备状态、当前页缓冲垫和最近 `20` 条运行事件。
- SSE 收到 `deviceStatus` 时只更新对应设备；收到 `cushionInfo` 时更新当前扫码结果，并重新查询当前页缓冲垫列表；收到 `operationEvent` 时把事件插入最近操作列表。
- 手动扫码在请求头中携带 `X-Operation-Id`。同一次扫码的 HTTP 结果、计数结果和 PLC 结果按该编号聚合，页面只弹出一条最终提示；状态 SSE 只更新页面数据，不再重复弹窗。
- 运行告警数字按“业务操作”统计，不按底层事件条数累计。右侧抽屉先显示每次操作的结论，需要排障时可展开查看该操作的完整事件过程。
- 浏览器原生 `EventSource` 会自动尝试重连。连接中断和恢复会在运行反馈条中提示，恢复后继续接收实时消息。
- 缓冲垫列表不是固定间隔轮询：首次进入、切换产线、翻页、查询、手动扫码或收到扫码 SSE 时重新查询，避免无操作时持续请求数据库。
- 前端内存中最多保留最近 `60` 条底层事件并展示最近 `20` 次业务操作；后端查询接口最多允许 `100` 条，数据库事件默认保留最近30天。

## 本地开发

```bash
npm install
npm run dev
```

开发服务默认监听 `8081`，`/api` 由 Webpack 代理到 `BACKEND_URL`，未设置时使用
`http://localhost:9001`。

```powershell
$env:BACKEND_URL = 'http://127.0.0.1:9001/'
npm run dev
```

## 测试与构建

```bash
npx eslint src/views/Layout/Running.vue src/modules/running test/unit/specs
npm run unit -- --runInBand
npm run build
```

生产包生成在 `dist/`。`static/config.js` 中的 `API_BASE_URL` 默认为空，浏览器会使用同源
`/api`，由 Nginx 转发到后端，部署时不需要写死服务器 IP。

Windows 离线安装包使用以下命令把当前 `dist/` 和后端 jar 同步到安装程序：

```powershell
powershell -ExecutionPolicy Bypass -File ..\bufferpad-installer\scripts\prepare-app.ps1
```

## 测试环境部署

| 项目 | 配置 |
| --- | --- |
| 测试服务器 | `192.0.2.4` |
| 访问地址 | `http://192.0.2.4:18088/` |
| 宿主机目录 | `/docker/nginx/bufferpad` |
| Nginx 容器 | `nginx-web` |
| 容器内目录 | `/usr/share/nginx/bufferpad` |
| 后端代理 | `/api/` -> `http://127.0.0.1:19001/` |
| SSE 代理 | `/api/sse/` -> `http://127.0.0.1:19001/sse/` |

已验证发布记录（2026-08-25）：前端提交 `96711f5`、后端提交 `829158a` 已部署；发布号为
`bufferpad-s7-20260825-003148`，前端备份位于
`/docker/nginx/backups/bufferpad-bufferpad-s7-20260825-003148`。主页返回 `200`，并通过
`/api/options/deviceTypes` 确认 S7-1200/S7-1500 类型已可用。

### 可重复发布步骤

本机重新构建并打包：

```powershell
npm run build
$release = "bufferpad-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
tar -cf "$env:TEMP\$release-frontend.tar" -C dist .
ssh root@192.0.2.4 "install -d -m 700 /docker/.$release"
scp "$env:TEMP\$release-frontend.tar" root@192.0.2.4:/docker/.$release/
```

服务器上备份当前文件；解压包时上例使用 `-C dist .`，因此无需再嵌套一层 `dist` 目录：

```bash
release=bufferpad-YYYYMMDD-HHMMSS
install -d /docker/nginx/backups
cp -a /docker/nginx/bufferpad "/docker/nginx/backups/bufferpad-$release"
tar -xf "/docker/.$release/$release-frontend.tar" -C /docker/nginx/bufferpad
```

部署后先比较 bind mount 两端首页哈希；若不一致，不重启共享 Nginx，直接把同一份文件流式同步到
`nginx-web` 当前可见目录：

```bash
sha256sum /docker/nginx/bufferpad/index.html
docker exec nginx-web sha256sum /usr/share/nginx/bufferpad/index.html
tar -cf - -C /docker/nginx/bufferpad . \
  | docker exec -i nginx-web tar -xf - -C /usr/share/nginx/bufferpad
curl -I http://127.0.0.1:18088/
curl http://127.0.0.1:18088/api/options/deviceTypes
```

若前端验收失败，使用对应备份恢复宿主机目录，并再次执行上面的流式同步命令。SSH 密码和数据库
密码只保存在受控服务器环境中，绝不能写入仓库或发布文档。

部署时先将当前 `/docker/nginx/bufferpad` 复制为带时间戳的备份，再把完整的 `dist/`
解压到临时目录，并将临时目录内容覆盖同步到 `/docker/nginx/bufferpad`。不要重命名或替换
`bufferpad` 目录本身：该目录是 Docker bind mount，容器会继续绑定重命名前的目录节点，
导致宿主机文件已更新但浏览器仍访问旧版本。也不要清理或覆盖 `/docker/nginx/html`，该目录
属于服务器上的其他系统。

示例：

```bash
stamp=$(date +%Y%m%d-%H%M%S)
cp -a /docker/nginx/bufferpad "/docker/nginx/backups/bufferpad-$stamp"
cp -a "/docker/nginx/.bufferpad-release-$stamp/." /docker/nginx/bufferpad/
```

服务器曾经替换过绑定目录，运行中的 `nginx-web` 可能仍持有历史目录 inode。部署后必须比较
宿主机与容器内首页哈希：

```bash
sha256sum /docker/nginx/bufferpad/index.html
docker exec nginx-web sha256sum /usr/share/nginx/bufferpad/index.html
```

如果两个哈希不同，说明容器仍在读取历史挂载。不要为此重启共享 Nginx，使用流式同步把同一份
内容写入容器当前可见目录：

```bash
tar -cf - -C /docker/nginx/bufferpad . \
  | docker exec -i nginx-web tar -xf - -C /usr/share/nginx/bufferpad
```

当前容器会立即生效；以后在计划维护窗口正常重启 `nginx-web` 后，容器会重新绑定已经更新好的
宿主机路径，该兼容步骤即可取消。

生产包使用文件哈希命名，覆盖同步后残留的旧哈希资源不会被新 `index.html` 引用，可在维护窗口
确认备份有效后再清理。

静态文件更新不需要重启 Nginx。只有修改 `/docker/nginx/conf/nginx.conf` 时才执行：

```bash
docker exec nginx-web nginx -t
docker exec nginx-web nginx -s reload
```

## 部署验收

```bash
curl -I http://127.0.0.1:18088/
curl http://127.0.0.1:19001/actuator/health
curl http://127.0.0.1:18088/api/options/deviceTypes
```

页面应返回 `200`，后端健康状态应为 `UP`。还需在浏览器确认运行监控能够加载设备状态、
最近操作事件，并能在手动扫码后显示计数结果或明确的 PLC 异常原因。运行页验收至少覆盖
`1920x1080`、`1366x768` 和 `1024x768`：页面应存在纵向滚动，设备摘要不能出现内部
滚动条，告警正文不能越界，空闲计数应显示 `--`，缓冲垫列表和底部分页必须可以正常查看。
