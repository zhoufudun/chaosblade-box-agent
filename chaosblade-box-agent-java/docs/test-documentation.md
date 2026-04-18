# 接口测试文档

> 验证需求: 17.8

## 1. HTTP API 接口测试

### 1.1 PingHandler (`/ping`)

**测试目的：** 验证健康检查接口正常返回成功响应。

**测试输入：**
```json
{
  "headers": { "FR": "C" },
  "params": {}
}
```

**预期输出：**
```json
{
  "Code": 200,
  "Success": true,
  "Error": null,
  "Result": "success"
}
```

**测试步骤：**
1. 构造空 Request 对象
2. 调用 `PingHandler.handle(request)`
3. 断言 `response.isSuccess() == true`
4. 断言 `response.getCode() == 200`

---

### 1.2 ChaosbladeHandler (`/chaosblade`)

**测试目的：** 验证 blade CLI 命令执行、参数校验、文件存在性检查、结果解析。

**测试用例 A — cmd 参数为空：**

**测试输入：**
```json
{
  "headers": {},
  "params": {}
}
```

**预期输出：**
```json
{
  "Code": 406,
  "Success": false,
  "Error": "`cmd`: parameter is empty"
}
```

**测试用例 B — blade 文件不存在：**

**测试输入：**
```json
{
  "headers": {},
  "params": { "cmd": "create cpu fullload" }
}
```

**预期输出：**
```json
{
  "Code": 600,
  "Success": false,
  "Error": "chaosblade file not found"
}
```

**测试用例 C — 正常执行：**

**测试步骤：**
1. Mock `BashUtil.execScript` 返回成功的 JSON 结果
2. 设置 `bladeBinPath` 指向存在的文件
3. 调用 `handle(request)`，其中 `cmd = "create cpu fullload"`
4. 断言返回 `Code == 200`
5. 验证 running map 中添加了对应 uid

**测试用例 D — parseResult 前缀容错：**

**测试步骤：**
1. 输入 `"some log prefix {"Code":200,"Success":true,"Result":"uid123"}"`
2. 调用 `ChaosbladeHandler.parseResult(input)`
3. 断言解析成功，`Code == 200`

---

### 1.3 UninstallHandler (`/uninstall`)

**测试目的：** 验证 Agent 卸载流程，包括 ctl 文件检查和执行。

**测试用例 A — ctl 文件不存在：**

**测试输入：**
```json
{
  "headers": {},
  "params": {}
}
```

**预期输出：**
```json
{
  "Code": 507,
  "Success": false,
  "Error": "`/path/to/chaosctl.sh`: ctl file not found"
}
```

**测试用例 B — ctl 执行失败：**

**测试步骤：**
1. Mock `ctlPathSupplier` 返回存在的文件路径
2. Mock `BashUtil.execScript` 返回失败
3. 断言 `Code == 508`

**测试用例 C — 正常卸载：**

**测试步骤：**
1. Mock `ctlPathSupplier` 返回存在的文件路径
2. Mock `BashUtil.execScript` 返回成功
3. 断言 `Code == 200`

---

### 1.4 UpdateApplicationHandler (`/updateApplication`)

**测试目的：** 验证应用实例名和组名更新，以及本地文件写入。

**测试输入：**
```json
{
  "headers": {},
  "params": {
    "appInstance": "my-app-instance",
    "appGroup": "my-app-group"
  }
}
```

**预期输出：**
```json
{
  "Code": 200,
  "Success": true,
  "Result": "success"
}
```

**测试步骤：**
1. 创建 `UpdateApplicationHandler`，传入 mock `optionsUpdater`
2. 构造包含 `appInstance` 和 `appGroup` 的 Request
3. 调用 `handle(request)`
4. 断言 `optionsUpdater` 被调用，参数为 `["my-app-instance", "my-app-group"]`
5. 断言 `.chaos.app` 文件内容包含正确的值

---

### 1.5 LitmusChaosHandler (`/litmuschaos`)

**测试目的：** 验证 LitmusChaos 实验的创建和销毁流程。

**测试用例 A — chaosAction 为空：**

**测试输入：**
```json
{
  "headers": {},
  "params": {}
}
```

**预期输出：**
```json
{
  "Code": 406,
  "Success": false,
  "Error": "`chaosAction`: parameter is empty"
}
```

**测试用例 B — LitmusChaos 未安装（version 为空）：**

**测试输入：**
```json
{
  "headers": {},
  "params": {
    "chaosAction": "create",
    "experimentType": "generic",
    "experimentName": "pod-delete",
    "appInfo": "{\"appkind\":\"deployment\",\"appns\":\"default\",\"applabel\":\"app=nginx\"}",
    "components": "{\"TOTAL_CHAOS_DURATION\":\"30\"}"
  }
}
```

**预期输出：**
```json
{
  "Code": 500,
  "Success": false,
  "Error": "server error, err: litmus operator not installed, please install first"
}
```

**测试用例 C — destroy 正常删除：**

**测试步骤：**
1. Mock K8s 客户端（fabric8 KubernetesServer）
2. 构造 destroy 请求，包含 `name` 和 `namespace`
3. 调用 `handle(request)`
4. 断言 `Code == 200`

---

### 1.6 InstallLitmusHandler (`/installLitmus`)

**测试目的：** 验证 LitmusChaos Helm 安装流程。

**测试用例 A — version 参数缺失：**

**测试输入：**
```json
{
  "headers": {},
  "params": {}
}
```

**预期输出：**
```json
{
  "Code": 407,
  "Success": false,
  "Error": "`version`: parameter less"
}
```

**测试用例 B — Helm 操作失败：**

**测试步骤：**
1. Mock `HelmClient.pullChart` 抛出异常
2. 构造包含 `version` 的 Request
3. 断言 `Code == 602`

**测试用例 C — 正常安装：**

**测试步骤：**
1. Mock `HelmClient` 所有方法正常返回
2. 构造 `version = "1.13.8"` 的 Request
3. 调用 `handle(request)`
4. 断言 `Code == 200`
5. 断言 `litmusChaosHandler.getLitmusChaosVersion() == "1.13.8"`

---

### 1.7 UninstallLitmusHandler (`/uninstallLitmus`)

**测试目的：** 验证 LitmusChaos Helm 卸载流程。

**测试用例 A — Helm 实例为空：**

**测试步骤：**
1. 构造 `helmClient = null` 的 Handler
2. 断言 `Code == 602`

**测试用例 B — 正常卸载：**

**测试步骤：**
1. Mock `HelmClient.uninstall` 正常返回
2. 调用 `handle(request)`
3. 断言 `Code == 200`
4. 断言 `litmusChaosHandler.getLitmusChaosVersion()` 为空

---

## 2. 传输层测试

### 2.1 Request/Response JSON 序列化

**测试目的：** 验证 Request 和 Response 对象的 JSON 序列化/反序列化正确性。

**测试步骤：**
1. 创建 Request，添加 headers `{"FR":"C","pid":"1234"}` 和 params `{"port":"19527"}`
2. 使用 `ObjectMapper` 序列化为 JSON 字符串
3. 反序列化回 Request 对象
4. 断言 headers 和 params 与原始值一致
5. 对 Response 执行相同的往返测试

**属性测试（Property 1）：** 对任意 headers/params 键值对，序列化→反序列化后应等价。

### 2.2 拦截器链验证

**测试目的：** 验证 TimestampInterceptor 和 AuthInterceptor 的入站/出站行为。

**入站测试步骤：**
1. 构造缺少 `ts` 字段的 Request → 断言返回 `Code == 401`
2. 构造包含无效 `ts` 的 Request → 断言返回 `Code == 401`
3. 构造缺少 `sn` 字段的 Request → 断言返回 `Code == 403`，错误信息含 "missing sign"
4. 构造签名不匹配的 Request → 断言返回 `Code == 403`
5. 构造正确签名的 Request → 断言通过（返回 null）

**出站测试步骤：**
1. 设置 AK/SK 后调用 `invoke(request)`
2. 断言 Request headers 包含 `ak` 和 `sn`
3. 断言 Request params 包含 `ts`（时间戳）
4. AK/SK 为空时调用 `invoke` → 断言返回 `Code == 405`

### 2.3 TransportClient invoke 流程

**测试目的：** 验证 TransportClient 的完整调用链。

**测试步骤：**
1. Mock `TransportChannel.doInvoker` 返回成功 JSON
2. 创建 `TransportClient(mockChannel)`
3. 调用 `invoke(uri, request, false)`
4. 断言 Request 中包含 `rid`（requestId）
5. 断言返回的 Response 正确反序列化

---

## 3. 连接层测试

### 3.1 ConnManager 注册和启动

**测试目的：** 验证处理器注册和并发启动。

**测试步骤：**
1. 创建 ConnManager
2. 注册多个 Mock ClientHandle
3. 调用 `start()`
4. 断言所有 ClientHandle 的 `start()` 被调用

### 3.2 ConnectHandler 注册请求

**测试目的：** 验证注册请求参数构建和响应处理。

**测试步骤：**
1. Mock TransportClient，捕获 invoke 参数
2. Mock 返回包含 `cid`、`uid`、`ak`、`sk` 的响应
3. 调用 `ConnectHandler.start()`
4. 断言请求包含 ip、pid、uid、instanceId、namespace、deviceType 等字段
5. 断言 ConnConfig 中 cid、uid 已更新
6. 断言 `AuthUtil.getAccessKey()` 和 `getSecureKey()` 已设置

### 3.3 HeartbeatHandler 心跳

**测试目的：** 验证心跳发送和 HBSnapshot 记录。

**测试步骤：**
1. Mock TransportClient
2. 创建 HeartbeatHandler，周期设为较短值
3. 调用 `start()`，等待至少一次心跳
4. 断言 `HB_SNAPSHOT_LIST.size() > 0`
5. Mock 心跳失败场景，断言记录 `success=false`

### 3.4 MetricHandler 指标上报

**测试目的：** 验证指标收集器管理和定时上报。

**测试步骤：**
1. 注册 Mock MetricCollector
2. 启动 MetricHandler
3. 断言 Collector 的 `report()` 被定时调用
4. 测试禁用某指标类型后不再上报

### 3.5 AsyncReportHandler 状态上报

**测试目的：** 验证异步状态上报请求构建。

**测试步骤：**
1. Mock TransportClient
2. 调用 `reportStatus("uid-123", "Success", "", "chaosblade", uri)`
3. 断言请求 params 包含 `uid=uid-123`、`status=Success`、`ToolType=chaosblade`

---

## 4. 工具层测试

### 4.1 AuthUtil 签名

**测试目的：** 验证 SHA256+Base64 签名生成与验证。

**测试步骤：**
1. 设置 AK/SK：`setKeys("testAK", "testSK")`
2. 对数据 `"hello"` 调用 `sign("hello")`，获得签名
3. 调用 `auth(签名, "hello")` → 断言返回 `true`
4. 调用 `auth("wrongSign", "hello")` → 断言返回 `false`
5. 使用 `signWithKey` 和 `authWithKey` 验证指定密钥签名

**属性测试（Property 3）：** 对任意数据和密钥，签名后验证应返回 true。
**属性测试（Property 4）：** 对任意数据，错误签名验证应返回 false。

### 4.2 FileUtil 文件操作

**测试目的：** 验证文件工具方法。

**测试步骤：**
1. `isExist` — 对存在/不存在的文件分别断言
2. `md5sum` — 创建临时文件，计算 MD5，与已知值比较
3. `checkMd5` — 传入正确/错误 MD5，断言返回值
4. `compressByGzip` / `decompressByGzip` — 压缩后解压，断言与原始字符串一致
5. `writeStringToFile` — 写入后读取，断言内容一致

**属性测试（Property 2）：** 对任意非空字符串，Gzip 压缩→解压应还原。
**属性测试（Property 21）：** MD5 计算与校验一致性。

### 4.3 LimitedList 容量限制

**测试目的：** 验证固定容量列表行为。

**测试步骤：**
1. 创建容量为 3 的 LimitedList
2. 依次 put 5 个元素
3. 断言 `size() == 3`
4. 使用 `foreach` 正向遍历，断言只包含最后 3 个元素
5. 使用 `foreachReverse` 反向遍历，断言顺序正确

**属性测试（Property 8）：** 对任意插入序列，size 永远不超过容量 N。

### 4.4 BashUtil 命令执行

**测试目的：** 验证 shell 命令执行和超时处理。

**测试步骤：**
1. 执行 `echo hello` → 断言 `isSuccess() == true`，output 包含 "hello"
2. 执行不存在的脚本 → 断言 `isSuccess() == false`
3. 超时测试：执行 `sleep 10`，超时设为 1s → 断言返回超时错误

---

## 5. 监控层测试

### 5.1 DefaultChecker 心跳检查

**测试目的：** 验证连续失败/成功阈值判断逻辑。

**测试用例 A — 连续 12 次失败触发停止：**

**测试步骤：**
1. 清空 `HB_SNAPSHOT_LIST`
2. 连续 put 12 个 `HBSnapshot(false)`
3. 调用 `checker.check()`
4. 断言 `action.isNeedStop() == true`
5. 断言 `action.getReason()` 包含 "heartbeat"

**测试用例 B — 停止后连续 3 次成功触发启动：**

**测试步骤：**
1. 先触发停止状态（12 次失败）
2. 连续 put 3 个 `HBSnapshot(true)`
3. 调用 `checker.check()`
4. 断言 `action.isNeedStart() == true`

**测试用例 C — 未达阈值不触发：**

**测试步骤：**
1. 连续 put 11 个 `HBSnapshot(false)`
2. 调用 `checker.check()`
3. 断言 `action.isNeedStop() == false`

**属性测试（Property 14）：** 连续 ≥12 次失败且未停止 → needStop=true。
**属性测试（Property 15）：** 已停止且连续 ≥3 次成功 → needStart=true。

### 5.2 Monitor 事件发送

**测试目的：** 验证 Monitor 向服务端发送 start/stop 事件。

**测试步骤：**
1. Mock TransportClient
2. 触发停止条件（12 次心跳失败）
3. 执行 Monitor 检查循环
4. 断言 TransportClient 收到 event=stop 的请求
5. 触发恢复条件（3 次心跳成功）
6. 断言 TransportClient 收到 event=start 的请求
