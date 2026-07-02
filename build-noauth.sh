#!/usr/bin/env bash
#
# build-noauth.sh - 构建"定制版" chaosagent tar 包
#
# 定制内容:
#   - 禁用 web/api/handler.go 里的 request 拦截器链（跳过 timestamp/auth 校验）
#     解决 v1.1.1 与老版 box SDK 之间的 401 兼容问题
#   - 保留 PR #24 的 SIGFPE 修复（os.UserHomeDir 替代 user.Current）
#   - 保留官方 chaosctl.sh / chaosrcv.sh / chaosblade 二进制不变
#
# 产物: chaosagent-<version>-<suffix>-linux_<arch>.tar.gz
#   默认 chaosagent-1.1.1-noauth-linux_amd64.tar.gz
#   目录布局与官方 1.1.1 tar 一致（chaos/agent + chaos/chaosctl.sh + chaos/chaosblade/...）
#
# 用法:
#   bash build-noauth.sh                          # 默认打 linux/amd64 版
#   bash build-noauth.sh --arch=arm64             # 打 linux/arm64
#   bash build-noauth.sh --suffix=custom          # 自定义后缀（默认 noauth）
#   bash build-noauth.sh --base-version=1.1.1     # 基座 tar 版本（默认 1.1.1）
#   bash build-noauth.sh --base-tar=/path/to/tar  # 显式指定本地基座 tar
#   bash build-noauth.sh --sync                   # 打完同步到 agent-deploy-4-node/
#   bash build-noauth.sh --skip-build             # 跳过 go build（复用 build/binary/agent）
#   bash build-noauth.sh --help
#
# 依赖:
#   - Go（编译期，任意 host，交叉编译）
#   - tar, curl, gzip, shasum, xattr（macOS）
#   - 网络可达 GitHub 或阿里 OSS（首次运行下载基座 tar）
#

set -euo pipefail

# ============ 默认配置 ============
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCH="amd64"
SUFFIX="noauth"
BASE_VERSION="1.1.1"
BASE_TAR=""
SKIP_BUILD=false
SYNC=false
SYNC_DIR="/Users/zhoufudun/IdeaProjects/LearnDemo/spring-amqp-maven/agent-deploy-4-node"

# 基座 tar 下载源（优先级从高到低）
BASE_URL_TEMPLATES=(
    "https://github.com/chaosblade-io/chaosblade-box-agent/releases/download/v{VERSION}/chaosagent-{VERSION}-linux_{ARCH}.tar.gz"
    "https://chaosblade.oss-cn-hangzhou.aliyuncs.com/platform/release/{VERSION}/chaosagent-{VERSION}-linux_{ARCH}.tar.gz"
)

# ============ 参数解析 ============
show_help() {
    sed -n '2,30p' "$0" | sed 's|^#\s\?||'
    exit 0
}

for arg in "$@"; do
    case "$arg" in
        --arch=*)         ARCH="${arg#*=}" ;;
        --suffix=*)       SUFFIX="${arg#*=}" ;;
        --base-version=*) BASE_VERSION="${arg#*=}" ;;
        --base-tar=*)     BASE_TAR="${arg#*=}" ;;
        --skip-build)     SKIP_BUILD=true ;;
        --sync)           SYNC=true ;;
        --sync-dir=*)     SYNC=true; SYNC_DIR="${arg#*=}" ;;
        -h|--help|help)   show_help ;;
        *)                echo "[ERROR] 未知参数: $arg"; echo "试试 --help"; exit 1 ;;
    esac
done

# 计算派生变量
case "$ARCH" in
    amd64|arm64) ;;
    *) echo "[ERROR] --arch 只支持 amd64 或 arm64（当前: $ARCH）"; exit 1 ;;
esac

OUT_NAME="chaosagent-${BASE_VERSION}-${SUFFIX}-linux_${ARCH}.tar.gz"
OUT_PATH="${SCRIPT_DIR}/${OUT_NAME}"
BUILD_BIN="${SCRIPT_DIR}/build/binary/agent"
CACHE_DIR="${SCRIPT_DIR}/build/cache"
WORK_DIR="${SCRIPT_DIR}/build/repack"

# ============ 辅助函数 ============
# 注意: log/err 都写 stderr，避免污染函数通过 stdout 返回的值（$(func_name)）
log() { echo "[$(date +%H:%M:%S)] $*" >&2; }
err() { echo "[ERROR] $*" >&2; }

# 下载基座 tar，多源 fallback
download_base_tar() {
    local version="$1"
    local arch="$2"
    local dest="${CACHE_DIR}/chaosagent-${version}-linux_${arch}.tar.gz"

    mkdir -p "$CACHE_DIR"

    # 已缓存 → 直接用
    if [ -f "$dest" ] && gzip -t "$dest" 2>/dev/null; then
        log "使用缓存的基座 tar: $dest"
        echo "$dest"
        return 0
    fi

    local idx=0
    local total=${#BASE_URL_TEMPLATES[@]}
    for tpl in "${BASE_URL_TEMPLATES[@]}"; do
        idx=$((idx + 1))
        local url="${tpl//\{VERSION\}/$version}"
        url="${url//\{ARCH\}/$arch}"
        log "[$idx/$total] 尝试下载: $url"
        if curl -fL --connect-timeout 15 --max-time 300 -o "$dest.tmp" "$url" 2>&1 | tail -5; then
            if gzip -t "$dest.tmp" 2>/dev/null; then
                mv "$dest.tmp" "$dest"
                log "下载成功，缓存到: $dest"
                echo "$dest"
                return 0
            else
                err "下载文件不是合法 gzip，可能是错误页"
                rm -f "$dest.tmp"
            fi
        else
            err "下载失败，尝试下一个源"
            rm -f "$dest.tmp"
        fi
    done
    return 1
}

# ============ Step 1: 编译 agent ============
if [ "$SKIP_BUILD" = true ]; then
    log "跳过 go build（--skip-build）"
    [ -f "$BUILD_BIN" ] || { err "$BUILD_BIN 不存在，无法跳过 build"; exit 1; }
else
    if ! command -v go >/dev/null 2>&1; then
        err "未找到 go 命令，请先安装 Go 或用 --skip-build"
        exit 1
    fi

    log "==== Step 1/4: 编译 agent (linux/${ARCH}, CGO_ENABLED=0) ===="
    log "go version: $(go version | awk '{print $3, $4}')"
    rm -f "$BUILD_BIN"
    mkdir -p "$(dirname "$BUILD_BIN")"

    (
        cd "$SCRIPT_DIR"
        CGO_ENABLED=0 GOOS=linux GOARCH="$ARCH" \
            go build -a -ldflags="-s -w" -o "$BUILD_BIN" cmd/chaos_agent.go
    )
    log "编译完成: $BUILD_BIN"
    log "sha256: $(shasum -a 256 "$BUILD_BIN" | awk '{print $1}')"
    log "大小:   $(du -h "$BUILD_BIN" | awk '{print $1}')"
fi

# ============ Step 2: 准备基座 tar ============
log ""
log "==== Step 2/4: 准备基座 tar (v${BASE_VERSION}) ===="

if [ -z "$BASE_TAR" ]; then
    if ! BASE_TAR=$(download_base_tar "$BASE_VERSION" "$ARCH"); then
        err "所有下载源均失败"
        err "可以显式指定本地基座: bash $(basename "$0") --base-tar=/path/to/chaosagent-${BASE_VERSION}-linux_${ARCH}.tar.gz"
        exit 1
    fi
else
    [ -f "$BASE_TAR" ] || { err "--base-tar 指定的文件不存在: $BASE_TAR"; exit 1; }
    gzip -t "$BASE_TAR" 2>/dev/null || { err "--base-tar 不是合法 gzip: $BASE_TAR"; exit 1; }
    log "使用本地基座 tar: $BASE_TAR"
fi
log "基座 sha256: $(shasum -a 256 "$BASE_TAR" | awk '{print $1}')"

# ============ Step 3: 替换 agent 二进制、重新打包 ============
log ""
log "==== Step 3/4: 替换 agent 二进制、重新打包 ===="

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
tar -xzf "$BASE_TAR" -C "$WORK_DIR"

# 基座应该有 chaos/ 顶层目录
[ -d "$WORK_DIR/chaos" ] || { err "基座 tar 布局异常，未找到 chaos/ 目录"; exit 1; }

# 替换 agent
cp "$BUILD_BIN" "$WORK_DIR/chaos/agent"
chmod +x "$WORK_DIR/chaos/agent"

# 校验一下 chaosctl.sh PID_FILE 是不是官方值（防止基座被污染）
if grep -q '^PID_FILE="/var/run/chaos.pid"' "$WORK_DIR/chaos/chaosctl.sh"; then
    log "chaosctl.sh PID_FILE = /var/run/chaos.pid (官方)"
else
    err "chaosctl.sh PID_FILE 不是官方 /var/run/chaos.pid，请检查基座 tar"
    grep 'PID_FILE=' "$WORK_DIR/chaos/chaosctl.sh" | head -3
    exit 1
fi

# 清 mac 扩展属性（避免 Linux 上 tar 解压 xattr warning）
xattr -cr "$WORK_DIR/chaos" 2>/dev/null || true

# 打新 tar
rm -f "$OUT_PATH"
tar --no-xattrs -czf "$OUT_PATH" -C "$WORK_DIR" chaos/

log "产物: $OUT_PATH"
log "大小: $(du -h "$OUT_PATH" | awk '{print $1}')"
log "sha256: $(shasum -a 256 "$OUT_PATH" | awk '{print $1}')"

# ============ Step 4: 同步到 agent-deploy-4-node（可选）============
if [ "$SYNC" = true ]; then
    log ""
    log "==== Step 4/4: 同步到 $SYNC_DIR ===="
    if [ ! -d "$SYNC_DIR" ]; then
        err "同步目录不存在: $SYNC_DIR"
        exit 1
    fi
    cp "$OUT_PATH" "$SYNC_DIR/"
    log "已复制到: $SYNC_DIR/$OUT_NAME"

    # 如果 agent-deploy-4-node/build-tar.sh 存在，顺便重打外层包
    if [ -x "$SYNC_DIR/build-tar.sh" ]; then
        log "触发 $SYNC_DIR/build-tar.sh ..."
        (cd "$SYNC_DIR" && bash build-tar.sh) | tail -10
    else
        log "未找到 $SYNC_DIR/build-tar.sh，跳过外层包重打"
    fi
else
    log ""
    log "==== Step 4/4: 跳过同步（未指定 --sync）===="
fi

# ============ 清理 + 汇报 ============
rm -rf "$WORK_DIR"

echo ""
echo "=========================================="
echo " 完成"
echo "=========================================="
echo "输出:   $OUT_PATH"
echo "sha256: $(shasum -a 256 "$OUT_PATH" | awk '{print $1}')"
echo ""
echo "分发方式:"
echo "  scp $OUT_PATH root@<host>:/home/lhsa/agent-deploy/agent-deploy-4-node/"
echo "  ssh root@<host> './deploy-agent.sh update --tar=/home/lhsa/agent-deploy/agent-deploy-4-node/$OUT_NAME --force'"
echo ""
echo "或者用 ansible 批量:"
echo "  ansible-playbook -i inventory.ini agent-deploy-4-node_ops.yml \\"
echo "      -e op=update -e update_tar=/home/lhsa/agent-deploy/agent-deploy-4-node/$OUT_NAME -e update_force=true"
