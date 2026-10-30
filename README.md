# 稿台

一个可以独立运行的素材制作演示站。用户选择模板、上传图片并提交任务；Java 后端按「素材检查 → 模拟生成 → 结果整理」异步执行，前端在任务处理期间继续可用，并能查看自己的历史结果。

制作过程不调用真实 AI。耗时和指定阶段失败由模拟配置控制。

题目原文保留在无扩展名的 `README` 中。

## 技术版本

| 部分 | 版本 |
| --- | --- |
| 前端 | Next.js 15.5.28、React 19.1.1、TypeScript 5.8.3 |
| 后端 | Java 21、Spring Boot 3.4.5、MyBatis Spring Boot 3.0.4 |
| 数据库 | MySQL 8.4 |
| 测试 | JUnit 5（随 Spring Boot）、Vitest 3.2.4 |
| 运行 | Docker Compose；前端 Node 22，后端 Eclipse Temurin 21 |

前端样式和异步状态都写在项目内，没有另选 UI 或状态库。任务进度使用短轮询，不依赖消息队列、Redis、SSE 或 WebSocket。

## 项目结构

```text
frontend/          Next.js App Router
backend/           Spring Boot + MyBatis
backend/src/main/resources/db/   表结构和种子数据
backend/src/main/resources/static/demo/   模板封面
docker-compose.yml
.github/workflows/ci.yml
```

后端按用户、模板、素材、任务分开。任务受理只创建记录并返回；`TaskWorker` 单线程领取 `PENDING` 任务，`TaskProcessor` 按阶段推进。查询服务只读数据库，不会把任务推到下一步。

## 用户身份

不接第三方账号。数据库预置两个用户：

| 请求头 `X-User-Id` | 显示名 |
| --- | --- |
| `u-linxia` | 林夏 |
| `u-zhouyu` | 周屿 |

顶部进入「我的」账号页后可以切换用户，选择保存在浏览器 `localStorage`。除 `GET /api/health` 和 `GET /api/users` 外，接口都要带 `X-User-Id`。任务按用户隔离，读取他人任务会得到 404。

切换用户时会中止尚未返回的列表和轮询请求，清空当前列表，再用新用户重新加载。页面之间的跟踪放在应用级，离开模板页不会把进行中的任务丢掉；过期响应带有会话代号，不会写回当前用户。

## 任务状态与后台执行

整体状态：

| status | 含义 |
| --- | --- |
| `PENDING` | 已受理，等待领取 |
| `PROCESSING` | 正在执行某个阶段 |
| `SUCCEEDED` | 三步都完成，`resultUrl` 可预览 |
| `FAILED` | 停在失败阶段，`failureReason` 可阅读 |

阶段 `stage`：`ASSET_CHECK`、`SIMULATE_GENERATE`、`RESULT_PACKAGING`。等待处理时 `stage` 为空。成功后阶段停在 `RESULT_PACKAGING`。失败后 `stage` 和 `failureStage` 都是失败的那一步。

执行方式：

1. `POST /api/tasks` 校验模板、素材归属、数量、类型、大小和文件是否存在，写入 `PENDING` 任务后立即返回 `202` 和任务编号。
2. 事务提交后唤醒工作线程。工作线程用条件更新把一条最老的 `PENDING` 领取成 `PROCESSING`。
3. 每一步先把阶段写入数据库，再等待配置的耗时，然后执行该步。查询只读状态，不能推动步骤。
4. 素材检查会再次确认文件仍在。模拟生成用等待表示处理。结果整理用 JDK 画一张 PNG，地址形如 `/media/results/{taskId}.png`。
5. 模拟配置指定的阶段在等待之后失败，后续步骤不再执行，也不写出结果图。
6. 状态更新都带 `WHERE status = 'PROCESSING'` 或 `PENDING`。成功或失败后，后续更新影响 0 行，任务不会回到处理中。

同一时间只跑一个任务，其余保持等待。多个任务可以连续提交。

## 主要接口

除健康检查和用户列表外，请求都带 `X-User-Id`。错误体为 `{ "error": "错误码", "message": "说明" }`。

`GET /api/users`

```json
[{ "id": "u-linxia", "displayName": "林夏" }]
```

`GET /api/templates?category=创意图片`

```json
{
  "categories": ["创意图片", "人像", "商品"],
  "templates": [{
    "id": "demo-postcard",
    "title": "旅行明信片",
    "category": "创意图片",
    "coverUrl": "/demo/postcard.svg",
    "description": "上传两张图片，模拟生成一张旅行明信片。",
    "requiredImageCount": 2,
    "acceptedMimeTypes": ["image/jpeg", "image/png"],
    "maxFileSizeBytes": 5242880
  }]
}
```

`GET /api/templates/{id}` 返回单个模板。不存在时 `404`。

`POST /api/assets`，`multipart/form-data` 字段名 `file`。

```json
{
  "id": "uuid",
  "url": "/media/uploads/uuid.png",
  "mimeType": "image/png",
  "sizeBytes": 12345,
  "originalFilename": "photo.png"
}
```

服务端按文件头识别 JPEG/PNG，不信任扩展名。单张上限 5MB。

`POST /api/tasks`

```json
{ "templateId": "demo-postcard", "assetIds": ["素材一", "素材二"] }
```

`202` 返回任务。此时 `status` 为 `PENDING`，`stage`、`failureReason`、`resultUrl`、`finishedAt` 为空。

`GET /api/tasks` 返回当前用户的任务，新的在前。`GET /api/tasks/{id}` 返回一条。字段：

```json
{
  "id": "uuid",
  "templateId": "demo-postcard",
  "templateTitle": "旅行明信片",
  "status": "PROCESSING",
  "stage": "SIMULATE_GENERATE",
  "failureStage": null,
  "failureReason": null,
  "resultUrl": null,
  "createdAt": "2026-04-01T00:00:00.000Z",
  "updatedAt": "2026-04-01T00:00:02.000Z",
  "finishedAt": null
}
```

时间是 UTC。

`GET /media/uploads/{file}` 与 `GET /media/results/{file}` 读取本地文件。文件名只允许字母、数字、点、下划线和连字符。

## 配置

示例都是演示凭据，不是真实账号。

| 变量 | 示例 | 作用 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/gaotai?characterEncoding=utf8&connectionTimeZone=UTC&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | 数据库 |
| `SPRING_DATASOURCE_USERNAME` | `gaotai` | 用户 |
| `SPRING_DATASOURCE_PASSWORD` | `gaotai` | 密码 |
| `STORAGE_ROOT` | `./data` 或容器内 `/data` | 上传和结果目录 |
| `SIMULATOR_STEP_DELAY_MS` | `2000` | 每步等待，0 到 120000 |
| `SIMULATOR_FAIL_STAGE` | `NONE` | 启动时的失败阶段 |
| `STUDIO_SIMULATOR_API_ENABLED` | `true` | 是否打开模拟配置接口 |
| `BACKEND_URL` | `http://localhost:8080` | 前端转发目标，Compose 里是 `http://backend:8080` |

模拟配置接口默认开启，方便演示。它不出现在页面上。关闭时设置 `STUDIO_SIMULATOR_API_ENABLED=false`。

`GET` 或 `PUT /api/dev/simulator`：

```json
{ "stepDelayMs": 2000, "failStage": "NONE" }
```

`failStage` 只能是 `NONE`、`ASSET_CHECK`、`SIMULATE_GENERATE`、`RESULT_PACKAGING`。修改立即作用于之后进入的步骤，并且对所有任务生效。进程重启后回到环境变量里的值。

## 启动

推荐：

```bash
docker compose up --build
```

浏览器打开 <http://localhost:3000>。后端健康检查是 <http://localhost:8080/api/health>。

MySQL 第一次启动时执行 `001_schema.sql` 和 `002_seed.sql`。数据在命名卷里，重复 `up` 不会重新灌入种子。需要清空数据库和素材时：

```bash
docker compose down -v
docker compose up --build
```

本地分开启动时，先准备 MySQL 8，创建数据库 `gaotai` 和用户 `gaotai` / `gaotai`，再执行上述两个 SQL。然后：

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm ci
npm run dev
```

前端开发地址仍是 <http://localhost:3000>，接口由 Next.js 转发到 `BACKEND_URL`，默认 `http://localhost:8080`。

## 演示

1. 打开首页，用分类切换模板，进入「旅行明信片」。
2. 上传两张不超过 5MB 的 JPEG 或 PNG。可以预览、替换和移除。数量、类型或大小不对时不能提交。
3. 点击「开始制作」。按钮从「提交中」恢复，并出现等待处理的任务。可以立刻再交下一个任务，或去看别的模板。
4. 打开「我的」。刷新页面后，未完成任务继续更新；完成后停止自动查询。成功任务显示结果图。
5. 打开「我的」，在制作记录里查看当前账号的任务。用「切换账号」选「周屿」，记录只剩周屿自己的。切回「林夏」后原任务还在。修改密码、注销、订阅管理和账单只是入口，点击后提示暂未开放。

指定阶段失败。下面的命令把每步改成 1 秒，并在模拟生成失败：

```bash
curl -s -X PUT http://localhost:8080/api/dev/simulator -H "Content-Type: application/json" -H "X-User-Id: u-linxia" -d '{"stepDelayMs":1000,"failStage":"SIMULATE_GENERATE"}'
```

PowerShell 请把 `curl` 换成 `curl.exe`，避免走到 `Invoke-WebRequest`。

再提交一个任务。它会先显示素材检查，再显示模拟生成，然后失败并给出原因，不会出现结果图。`ASSET_CHECK` 和 `RESULT_PACKAGING` 同样可指定。

恢复成功路径：

```bash
curl -s -X PUT http://localhost:8080/api/dev/simulator -H "Content-Type: application/json" -H "X-User-Id: u-linxia" -d '{"stepDelayMs":2000,"failStage":"NONE"}'
```

把 `stepDelayMs` 调到 `5000` 或 `0`，可以拉长或缩短处理中的观察时间。配置是全局的，演示失败后要改回 `NONE`。

查询异常和任务失败不是一回事。任务进行中时停掉后端，记录页会保留原来的状态和阶段，并提示暂时无法查询；任务不会被标成失败。后端恢复后点「重新查询」。

## 测试与 CI

```bash
cd backend && mvn -B verify
cd frontend && npm ci && npm run lint && npm run typecheck && npm test && npm run build
```

后端测试覆盖步骤顺序、三个阶段的失败中断、缺文件时停在素材检查、查询不推进状态、成功任务不会回到处理中，以及领取顺序。前端测试覆盖多个任务互不覆盖、查询失败不改任务状态、过期响应被丢弃、切换用户后不保留上一位用户的任务。

GitHub Actions 在 push 和 pull request 时分别运行前端 lint、类型检查、测试、构建，以及 `mvn verify`。

## 部署与回滚

演示环境就是 Compose。更新时在项目目录执行 `docker compose up --build`。回滚时检出上一版代码，再执行一次 `docker compose up --build`。

卷 `mysql-data` 保存任务和用户，`studio-data` 保存上传文件和结果图。`docker compose down` 不删卷。`docker compose down -v` 会删掉数据库和素材，只在需要重置演示数据时使用。

没有自动发布，也没有生产环境操作步骤。容器里的后端以非 root 用户 `app` 运行。

## 完成程度与限制

已完成题目里的模板浏览、上传校验、异步三步、失败与耗时控制、多任务、刷新恢复、用户隔离、测试、CI 和 Compose。实际投入约 2 天，由一人完成。

已知限制：

- 服务重启后，还没被领取的 `PENDING` 任务会被工作线程继续领走。已经进入 `PROCESSING` 的任务停在当前阶段，不会断点续跑，也不会自动改成失败。题目不要求重启恢复。
- 重复点击若绕过页面，会创建多个任务。没有幂等键，也没有失败后的重试按钮。
- 模拟失败和耗时对所有后续步骤全局生效，不按任务单独配置，重启后恢复为环境变量。
- 素材和结果地址使用随机文件名，接口本身不再校验登录用户。知道地址即可访问。
- 工作线程一次只处理一个任务。
- 模拟配置接口默认开启。正式使用时应关掉。
- 结果图是后端绘制的示意 PNG，不是对上传图片做真实合成。

## AI 使用说明

这个实现使用了 Cursor 里的 AI 编程助手。做法是先读完题目，把工作拆成后端阶段机、前端异步状态、交付配置三块；约束（查询不能推进任务、查询失败不是任务失败、关页面不能取消后台任务）写进接口和测试，再生成代码。验证用 `mvn test`、前端 `vitest`、lint、类型检查和构建。测试编译时内存数据库不能同时实现多个同名 `findById`，已拆开后重新通过。

没有提交聊天记录或账号。封面 SVG、页面和业务代码都是为本项目编写的，没有套用现成业务脚手架。Spring Boot 与 Next.js 使用各自的官方框架。

## 素材

五个模板和两个用户写在 `backend/src/main/resources/db/002_seed.sql`。封面是 `backend/src/main/resources/static/demo/` 下的 SVG，由本项目绘制，不依赖外网图片。
