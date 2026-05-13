#!/bin/bash
#
# ============================================================
# 家庭便当 - 一键打包部署脚本
# ============================================================
# 使用方法:
#   ./deploy.sh [命令]
#
# 可用命令:
#   build     - 仅打包（后端jar + 前端小程序）
#   deploy    - 打包并部署到远程服务器
#   restart   - 仅重启远程服务
#   stop      - 停止远程服务
#   status    - 查看远程服务状态
#   init-db   - 初始化远程数据库
#   help      - 显示帮助信息
#
# ============================================================

set -e

# ============================================================
# 【配置区】- 修改以下常量即可完成部署配置
# ============================================================

# ---------- 服务器配置 ----------
REMOTE_HOST="your-server-ip"          # 远程服务器IP/域名
REMOTE_PORT=22                         # SSH端口
REMOTE_USER="root"                     # SSH用户名
REMOTE_KEY=""                          # SSH私钥路径（为空则使用密码登录）
DEPLOY_DIR="/opt/home-made-lunch"      # 远程部署目录

# ---------- 后端配置 ----------
SERVER_PORT=8080                       # 后端服务端口
JAVA_OPTS="-Xms256m -Xmx512m"         # JVM参数

# ---------- 数据库配置 ----------
DB_HOST="localhost"                    # 数据库地址
DB_PORT=3306                           # 数据库端口
DB_NAME="home_made_lunch"             # 数据库名
DB_USER="root"                         # 数据库用户名
DB_PASSWORD=""                         # 数据库密码

# ---------- 微信小程序配置 ----------
WECHAT_APPID="your-appid"             # 微信小程序AppID
WECHAT_SECRET="your-secret"           # 微信小程序AppSecret

# ---------- 小程序前端配置 ----------
API_BASE_URL="https://your-domain.com/api"  # 生产环境API地址

# ---------- 构建配置 ----------
SKIP_TESTS=true                        # 是否跳过测试
OUTPUT_DIR="./target/deploy"           # 打包输出目录

# ============================================================
# 【以下为脚本逻辑，一般无需修改】
# ============================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 获取项目根目录
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# SSH命令构建
build_ssh_cmd() {
    local cmd="ssh -p $REMOTE_PORT"
    if [ -n "$REMOTE_KEY" ]; then
        cmd="$cmd -i $REMOTE_KEY"
    fi
    cmd="$cmd $REMOTE_USER@$REMOTE_HOST"
    echo "$cmd"
}

# SCP命令构建
build_scp_cmd() {
    local cmd="scp -P $REMOTE_PORT"
    if [ -n "$REMOTE_KEY" ]; then
        cmd="$cmd -i $REMOTE_KEY"
    fi
    echo "$cmd"
}

# ============================================================
# 后端打包
# ============================================================
build_backend() {
    log_step "开始打包后端 Spring Boot 应用..."

    local mvn_args="clean package -DskipTests=$SKIP_TESTS"

    # 使用Maven打包
    if command -v mvn &> /dev/null; then
        mvn $mvn_args
    elif [ -f "./mvnw" ]; then
        ./mvnw $mvn_args
    else
        log_error "未找到 Maven，请安装 Maven 或使用 mvnw"
        exit 1
    fi

    # 检查jar包是否生成
    local jar_file=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
    if [ -z "$jar_file" ]; then
        log_error "后端打包失败，未找到jar文件"
        exit 1
    fi

    log_info "后端打包成功: $jar_file"
    echo "$jar_file"
}

# ============================================================
# 前端配置替换（生成生产环境配置）
# ============================================================
build_frontend() {
    log_step "开始处理前端小程序配置..."

    local mini_dir="$PROJECT_DIR/miniprogram"
    local output_mini_dir="$OUTPUT_DIR/miniprogram"

    # 复制小程序源码到输出目录
    rm -rf "$output_mini_dir"
    cp -r "$mini_dir" "$output_mini_dir"

    # 替换 app.js 中的 API 地址
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s|baseUrl: '.*'|baseUrl: '${API_BASE_URL}'|g" "$output_mini_dir/app.js"
    else
        # Linux
        sed -i "s|baseUrl: '.*'|baseUrl: '${API_BASE_URL}'|g" "$output_mini_dir/app.js"
    fi

    # 替换 project.config.json 中的 appid
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|\"appid\": \".*\"|\"appid\": \"${WECHAT_APPID}\"|g" "$output_mini_dir/project.config.json"
    else
        sed -i "s|\"appid\": \".*\"|\"appid\": \"${WECHAT_APPID}\"|g" "$output_mini_dir/project.config.json"
    fi

    log_info "前端小程序配置替换完成"
    log_info "  API地址: $API_BASE_URL"
    log_info "  AppID: $WECHAT_APPID"
    log_info "  输出目录: $output_mini_dir"
}

# ============================================================
# 生成生产环境 application.yml
# ============================================================
generate_config() {
    log_step "生成生产环境配置文件..."

    mkdir -p "$OUTPUT_DIR"

    cat > "$OUTPUT_DIR/application-prod.yml" << EOF
server:
  port: ${SERVER_PORT}
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

wechat:
  appid: ${WECHAT_APPID}
  secret: ${WECHAT_SECRET}

logging:
  level:
    com.homemadelunch: info
  file:
    name: ${DEPLOY_DIR}/logs/app.log
  logback:
    rollingpolicy:
      max-file-size: 50MB
      max-history: 30
EOF

    log_info "配置文件已生成: $OUTPUT_DIR/application-prod.yml"
}

# ============================================================
# 生成服务管理脚本
# ============================================================
generate_service_script() {
    log_step "生成服务管理脚本..."

    cat > "$OUTPUT_DIR/service.sh" << 'SCRIPT_EOF'
#!/bin/bash
# 家庭便当 - 服务管理脚本

APP_NAME="home-made-lunch"
APP_DIR="DEPLOY_DIR_PLACEHOLDER"
JAR_FILE="$APP_DIR/app.jar"
PID_FILE="$APP_DIR/app.pid"
LOG_FILE="$APP_DIR/logs/app.log"
JAVA_OPTS="JAVA_OPTS_PLACEHOLDER"

start() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            echo "$APP_NAME 已在运行 (PID: $pid)"
            return 0
        fi
    fi

    mkdir -p "$APP_DIR/logs"
    echo "启动 $APP_NAME ..."
    nohup java $JAVA_OPTS -jar "$JAR_FILE" \
        --spring.profiles.active=prod \
        --spring.config.additional-location="$APP_DIR/application-prod.yml" \
        > "$APP_DIR/logs/startup.log" 2>&1 &

    echo $! > "$PID_FILE"
    sleep 2

    if kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "$APP_NAME 启动成功 (PID: $(cat $PID_FILE))"
    else
        echo "$APP_NAME 启动失败，请查看日志: $APP_DIR/logs/startup.log"
        return 1
    fi
}

stop() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            echo "停止 $APP_NAME (PID: $pid) ..."
            kill "$pid"
            # 等待进程退出
            for i in $(seq 1 30); do
                if ! kill -0 "$pid" 2>/dev/null; then
                    echo "$APP_NAME 已停止"
                    rm -f "$PID_FILE"
                    return 0
                fi
                sleep 1
            done
            # 强制杀死
            echo "强制停止 $APP_NAME ..."
            kill -9 "$pid"
            rm -f "$PID_FILE"
        else
            echo "$APP_NAME 未在运行"
            rm -f "$PID_FILE"
        fi
    else
        echo "$APP_NAME 未在运行"
    fi
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            echo "$APP_NAME 正在运行 (PID: $pid)"
            return 0
        fi
    fi
    echo "$APP_NAME 未在运行"
    return 1
}

log() {
    tail -f "$LOG_FILE"
}

case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) restart ;;
    status)  status ;;
    log)     log ;;
    *)
        echo "用法: $0 {start|stop|restart|status|log}"
        exit 1
        ;;
esac
SCRIPT_EOF

    # 替换占位符
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|DEPLOY_DIR_PLACEHOLDER|${DEPLOY_DIR}|g" "$OUTPUT_DIR/service.sh"
        sed -i '' "s|JAVA_OPTS_PLACEHOLDER|${JAVA_OPTS}|g" "$OUTPUT_DIR/service.sh"
    else
        sed -i "s|DEPLOY_DIR_PLACEHOLDER|${DEPLOY_DIR}|g" "$OUTPUT_DIR/service.sh"
        sed -i "s|JAVA_OPTS_PLACEHOLDER|${JAVA_OPTS}|g" "$OUTPUT_DIR/service.sh"
    fi

    chmod +x "$OUTPUT_DIR/service.sh"
    log_info "服务管理脚本已生成: $OUTPUT_DIR/service.sh"
}

# ============================================================
# 完整打包流程
# ============================================================
do_build() {
    log_info "=========================================="
    log_info "  家庭便当 - 开始打包"
    log_info "=========================================="

    # 创建输出目录
    rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"

    # 1. 打包后端
    local jar_file=$(build_backend)

    # 2. 复制jar到输出目录
    cp "$jar_file" "$OUTPUT_DIR/app.jar"

    # 3. 生成生产环境配置
    generate_config

    # 4. 生成服务管理脚本
    generate_service_script

    # 5. 处理前端小程序
    build_frontend

    # 6. 复制数据库初始化脚本
    cp "$PROJECT_DIR/src/main/resources/schema.sql" "$OUTPUT_DIR/schema.sql"

    log_info "=========================================="
    log_info "  打包完成！"
    log_info "=========================================="
    log_info ""
    log_info "输出目录: $OUTPUT_DIR"
    log_info "目录结构:"
    log_info "  ├── app.jar                  # 后端可执行jar"
    log_info "  ├── application-prod.yml     # 生产环境配置"
    log_info "  ├── service.sh               # 服务管理脚本"
    log_info "  ├── schema.sql               # 数据库初始化SQL"
    log_info "  └── miniprogram/             # 小程序源码（已替换生产配置）"
    log_info ""
    log_info "小程序需使用微信开发者工具上传发布"
}

# ============================================================
# 部署到远程服务器
# ============================================================
do_deploy() {
    # 先打包
    do_build

    log_step "开始部署到远程服务器 $REMOTE_USER@$REMOTE_HOST..."

    local ssh_cmd=$(build_ssh_cmd)
    local scp_cmd=$(build_scp_cmd)

    # 创建远程目录
    $ssh_cmd "mkdir -p $DEPLOY_DIR/logs $DEPLOY_DIR/backup"

    # 备份旧版本
    log_info "备份旧版本..."
    $ssh_cmd "if [ -f $DEPLOY_DIR/app.jar ]; then cp $DEPLOY_DIR/app.jar $DEPLOY_DIR/backup/app-\$(date +%Y%m%d%H%M%S).jar; fi"

    # 停止旧服务
    log_info "停止旧服务..."
    $ssh_cmd "if [ -f $DEPLOY_DIR/service.sh ]; then bash $DEPLOY_DIR/service.sh stop; fi" || true

    # 上传文件
    log_info "上传部署文件..."
    $scp_cmd "$OUTPUT_DIR/app.jar" "$REMOTE_USER@$REMOTE_HOST:$DEPLOY_DIR/app.jar"
    $scp_cmd "$OUTPUT_DIR/application-prod.yml" "$REMOTE_USER@$REMOTE_HOST:$DEPLOY_DIR/application-prod.yml"
    $scp_cmd "$OUTPUT_DIR/service.sh" "$REMOTE_USER@$REMOTE_HOST:$DEPLOY_DIR/service.sh"
    $scp_cmd "$OUTPUT_DIR/schema.sql" "$REMOTE_USER@$REMOTE_HOST:$DEPLOY_DIR/schema.sql"

    # 设置权限
    $ssh_cmd "chmod +x $DEPLOY_DIR/service.sh"

    # 启动服务
    log_info "启动新服务..."
    $ssh_cmd "bash $DEPLOY_DIR/service.sh start"

    # 等待并检查状态
    sleep 3
    $ssh_cmd "bash $DEPLOY_DIR/service.sh status"

    log_info "=========================================="
    log_info "  部署完成！"
    log_info "=========================================="
    log_info "  服务地址: http://$REMOTE_HOST:$SERVER_PORT/api"
    log_info "  管理命令: ssh $REMOTE_USER@$REMOTE_HOST 'bash $DEPLOY_DIR/service.sh {start|stop|restart|status|log}'"
    log_info ""
    log_info "  小程序请使用微信开发者工具打开 $OUTPUT_DIR/miniprogram 目录上传发布"
}

# ============================================================
# 远程重启服务
# ============================================================
do_restart() {
    log_step "重启远程服务..."
    local ssh_cmd=$(build_ssh_cmd)
    $ssh_cmd "bash $DEPLOY_DIR/service.sh restart"
}

# ============================================================
# 停止远程服务
# ============================================================
do_stop() {
    log_step "停止远程服务..."
    local ssh_cmd=$(build_ssh_cmd)
    $ssh_cmd "bash $DEPLOY_DIR/service.sh stop"
}

# ============================================================
# 查看远程服务状态
# ============================================================
do_status() {
    local ssh_cmd=$(build_ssh_cmd)
    $ssh_cmd "bash $DEPLOY_DIR/service.sh status"
}

# ============================================================
# 初始化远程数据库
# ============================================================
do_init_db() {
    log_step "初始化远程数据库..."

    local ssh_cmd=$(build_ssh_cmd)
    local scp_cmd=$(build_scp_cmd)

    # 上传SQL文件
    $scp_cmd "$PROJECT_DIR/src/main/resources/schema.sql" "$REMOTE_USER@$REMOTE_HOST:$DEPLOY_DIR/schema.sql"

    # 执行SQL
    local mysql_cmd="mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER}"
    if [ -n "$DB_PASSWORD" ]; then
        mysql_cmd="$mysql_cmd -p${DB_PASSWORD}"
    fi

    $ssh_cmd "$mysql_cmd -e 'CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'"
    $ssh_cmd "$mysql_cmd ${DB_NAME} < $DEPLOY_DIR/schema.sql"

    log_info "数据库初始化完成"
}

# ============================================================
# 显示帮助信息
# ============================================================
do_help() {
    echo ""
    echo "家庭便当 - 打包部署脚本"
    echo ""
    echo "用法: ./deploy.sh [命令]"
    echo ""
    echo "可用命令:"
    echo "  build     仅打包（后端jar + 前端小程序配置替换）"
    echo "  deploy    打包并部署到远程服务器"
    echo "  restart   重启远程服务"
    echo "  stop      停止远程服务"
    echo "  status    查看远程服务状态"
    echo "  init-db   初始化远程数据库"
    echo "  help      显示此帮助信息"
    echo ""
    echo "首次部署步骤:"
    echo "  1. 修改脚本开头的配置常量"
    echo "  2. ./deploy.sh init-db    # 初始化数据库"
    echo "  3. ./deploy.sh deploy     # 打包并部署"
    echo "  4. 使用微信开发者工具打开 target/deploy/miniprogram 上传小程序"
    echo ""
}

# ============================================================
# 主入口
# ============================================================
case "${1:-help}" in
    build)    do_build ;;
    deploy)   do_deploy ;;
    restart)  do_restart ;;
    stop)     do_stop ;;
    status)   do_status ;;
    init-db)  do_init_db ;;
    help)     do_help ;;
    *)
        log_error "未知命令: $1"
        do_help
        exit 1
        ;;
esac
