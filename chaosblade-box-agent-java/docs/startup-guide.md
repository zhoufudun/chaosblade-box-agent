# ChaosBlade Box Agent Java - 启动与停止指南

## 前置条件

- JDK 8+
- Maven 3.6+
- ChaosBlade 工具已安装到 `/opt/chaosblade/blade`

## 构建

```bash
cd chaosblade-box-agent-java
mvn package -pl chaosblade-box-agent-bootstrap -am -DskipTests -q
```

## 启动

### 前台启动

```bash
cd chaosblade-box-agent-java
java -jar chaosblade-box-agent-bootstrap/target/chaosblade-box-agent-bootstrap-1.0.0-SNAPSHOT.jar
```

### 后台启动

```bash
cd chaosblade-box-agent-java
nohup java -jar chaosblade-box-agent-bootstrap/target/chaosblade-box-agent-bootstrap-1.0.0-SNAPSHOT.jar > agent.log 2>&1 &
```

### 自定义配置启动

可通过命令行参数覆盖 `application.properties` 中的配置：

```bash
java -jar chaosblade-box-agent-bootstrap/target/chaosblade-box-agent-bootstrap-1.0.0-SNAPSHOT.jar \
  --agent.transport-endpoint=192.168.1.100:7001 \
  --agent.license=你的license \
  --agent.local-ip=你的IP \
  --agent.application-instance=你的应用名 \
  --agent.application-group=你的应用分组
```

## 停止

### 通过 PID 文件停止

```bash
kill $(cat /var/run/chaos.pid)
```

### 通过端口查找并停止

```bash
# 查找进程
lsof -i :19528 -t

# 停止
kill $(lsof -i :19528 -t)

# 强制停止
kill -9 $(lsof -i :19528 -t)
```

## 验证

```bash
# 检查进程是否存活
curl -s -X POST http://localhost:19528/ping \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'body={}'

# 预期返回
# {"Code":200,"Success":true,"Error":"","Result":"success"}
```

## 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | Agent HTTP 端口 | 19528 |
| `agent.transport-endpoint` | Box Server 地址 | localhost:7001 |
| `agent.license` | 注册许可证 | - |
| `agent.local-ip` | Agent IP（留空自动检测） | - |
| `agent.application-instance` | 应用名称 | chaos-default-app |
| `agent.application-group` | 应用分组 | chaos-default-app-group |
| `agent.heartbeat-period` | 心跳间隔 | 5s |
| `agent.mode` | 运行模式（host/k8s） | host |
