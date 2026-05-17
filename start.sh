#!/bin/bash
# ============================================================
# home-made-lunch 后端服务启动脚本
# 适用于 2C2G 云服务器
# 用法: ./start.sh {start|stop|restart|status|run|deploy|update|build}
# ============================================================

# 应用配置
APP_NAME="home-made-lunch"
JAR_NAME="home-made-lunch-1.0.0.jar"
APP_HOME=$(cd $(dirname $0); pwd)
JAR_PATH="${APP_HOME}/target/${JAR_NAME}"
LOG_DIR="${APP_HOME}/logs"
LOG_FILE="${LOG_DIR}/app.log"
PID_FILE="${APP_HOME}/${APP_NAME}.pid"

# JVM参数（针对2C2G优化）
JAVA_OPTS="-server"
JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS} -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof"
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS} -Duser.timezone=Asia/Shanghai"

# Spring配置（可按需修改profile）
SPRING_OPTS="--spring.profiles.active=prod"

# Git配置
GIT_BRANCH="main"

# 创建日志目录
mkdir -p ${LOG_DIR}

# 获取PID
get_pid() {
    if [ -f "${PID_FILE}" ]; then
        local pid=$(cat "${PID_FILE}")
        if [ -n "${pid}" ] && kill -0 ${pid} 2>/dev/null; then
            echo ${pid}
            return
        fi
    fi
    # PID文件不存在或进程已死，尝试通过进程名查找
    local pid=$(ps -ef | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')
    echo ${pid}
}

# 启动
start() {
    local pid=$(get_pid)
    if [ -n "${pid}" ]; then
        echo "[INFO] ${APP_NAME} 已在运行中 (PID: ${pid})"
        return 0
    fi

    if [ ! -f "${JAR_PATH}" ]; then
        echo "[ERROR] JAR包不存在: ${JAR_PATH}"
        echo "[INFO] 请先执行 mvn clean package -DskipTests 进行打包"
        return 1
    fi

    echo "[INFO] 正在启动 ${APP_NAME} ..."
    nohup java ${JAVA_OPTS} -jar ${JAR_PATH} ${SPRING_OPTS} > ${LOG_FILE} 2>&1 &
    local new_pid=$!
    echo ${new_pid} > ${PID_FILE}

    # 等待启动完成（最多30秒）
    echo -n "[INFO] 等待启动"
    for i in $(seq 1 30); do
        sleep 1
        echo -n "."
        if ! kill -0 ${new_pid} 2>/dev/null; then
            echo ""
            echo "[ERROR] 启动失败，请查看日志: ${LOG_FILE}"
            rm -f ${PID_FILE}
            return 1
        fi
        # 检查端口是否已监听
        if netstat -tlnp 2>/dev/null | grep -q ":8080 " || ss -tlnp | grep -q ":8080 "; then
            echo ""
            echo "[INFO] ${APP_NAME} 启动成功 (PID: ${new_pid})"
            return 0
        fi
    done
    echo ""
    echo "[WARN] 启动超时，请检查日志: ${LOG_FILE}"
    echo "[INFO] 进程 PID: ${new_pid}，可能仍在启动中..."
}

# 停止
stop() {
    local pid=$(get_pid)
    if [ -z "${pid}" ]; then
        echo "[INFO] ${APP_NAME} 未在运行"
        rm -f ${PID_FILE}
        return 0
    fi

    echo "[INFO] 正在停止 ${APP_NAME} (PID: ${pid}) ..."
    kill ${pid}

    # 等待进程退出（最多15秒）
    for i in $(seq 1 15); do
        sleep 1
        if ! kill -0 ${pid} 2>/dev/null; then
            echo "[INFO] ${APP_NAME} 已停止"
            rm -f ${PID_FILE}
            return 0
        fi
    done

    # 强制杀死
    echo "[WARN] 优雅停止超时，强制终止..."
    kill -9 ${pid} 2>/dev/null
    sleep 1
    rm -f ${PID_FILE}
    echo "[INFO] ${APP_NAME} 已强制停止"
}

# 重启
restart() {
    stop
    sleep 2
    start
}

# 状态
status() {
    local pid=$(get_pid)
    if [ -n "${pid}" ]; then
        echo "[INFO] ${APP_NAME} 正在运行 (PID: ${pid})"
        echo "[INFO] 内存使用:"
        ps -p ${pid} -o pid,vsz,rss,pcpu,pmem,etime,cmd --no-headers 2>/dev/null | awk '{
            printf "  PID: %s\n  虚拟内存: %.1fMB\n  物理内存: %.1fMB\n  CPU: %s%%\n  内存占比: %s%%\n  运行时间: %s\n", $1, $2/1024, $3/1024, $4, $5, $6
        }'
    else
        echo "[INFO] ${APP_NAME} 未在运行"
    fi
}

# 智能启动：进程存在则重启，否则启动
smart_start() {
    local pid=$(get_pid)
    if [ -n "${pid}" ]; then
        echo "[INFO] 检测到 ${APP_NAME} 正在运行 (PID: ${pid})，执行重启..."
        restart
    else
        echo "[INFO] ${APP_NAME} 未在运行，执行启动..."
        start
    fi
}

# 更新代码
update() {
    echo "[INFO] 正在拉取最新代码 (分支: ${GIT_BRANCH}) ..."
    cd ${APP_HOME}

    # 检查是否是git仓库
    if [ ! -d ".git" ]; then
        echo "[ERROR] 当前目录不是Git仓库"
        return 1
    fi

    # 暂存本地修改（如果有）
    local has_changes=$(git status --porcelain 2>/dev/null)
    if [ -n "${has_changes}" ]; then
        echo "[WARN] 检测到本地未提交的修改，自动暂存..."
        git stash
    fi

    # 拉取最新代码
    git pull origin ${GIT_BRANCH}
    if [ $? -ne 0 ]; then
        echo "[ERROR] 代码拉取失败"
        # 恢复暂存
        if [ -n "${has_changes}" ]; then
            git stash pop 2>/dev/null
        fi
        return 1
    fi

    # 恢复暂存的修改
    if [ -n "${has_changes}" ]; then
        echo "[INFO] 恢复本地暂存的修改..."
        git stash pop 2>/dev/null
    fi

    echo "[INFO] 代码更新完成"
    echo "[INFO] 最新提交: $(git log -1 --pretty=format:'%h - %s (%ci)')"
}

# Maven打包
build() {
    echo "[INFO] 正在执行 Maven 打包..."
    cd ${APP_HOME}

    # 检查mvn命令是否存在
    if ! command -v mvn &>/dev/null; then
        echo "[ERROR] mvn 命令未找到，请先安装 Maven"
        return 1
    fi

    # 执行打包（2C2G服务器限制编译线程数，避免OOM）
    mvn clean package -DskipTests -T 1C -q
    if [ $? -ne 0 ]; then
        echo "[ERROR] Maven 打包失败"
        return 1
    fi

    # 验证jar包是否生成
    if [ ! -f "${JAR_PATH}" ]; then
        echo "[ERROR] 打包完成但JAR包未找到: ${JAR_PATH}"
        return 1
    fi

    local jar_size=$(du -h ${JAR_PATH} | awk '{print $1}')
    echo "[INFO] Maven 打包成功: ${JAR_NAME} (${jar_size})"
}

# 一键部署：更新代码 + 打包 + 重启
deploy() {
    echo "=========================================="
    echo "  ${APP_NAME} 一键部署"
    echo "=========================================="
    echo ""

    # Step 1: 更新代码
    echo "[Step 1/3] 更新代码"
    echo "------------------------------------------"
    update
    if [ $? -ne 0 ]; then
        echo "[ERROR] 部署中止：代码更新失败"
        return 1
    fi
    echo ""

    # Step 2: Maven打包
    echo "[Step 2/3] Maven打包"
    echo "------------------------------------------"
    build
    if [ $? -ne 0 ]; then
        echo "[ERROR] 部署中止：打包失败"
        return 1
    fi
    echo ""

    # Step 3: 重启服务
    echo "[Step 3/3] 重启服务"
    echo "------------------------------------------"
    smart_start
    if [ $? -ne 0 ]; then
        echo "[ERROR] 部署中止：服务启动失败"
        return 1
    fi
    echo ""

    echo "=========================================="
    echo "  部署完成！"
    echo "=========================================="
}

# 主入口
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    run)
        smart_start
        ;;
    update)
        update
        ;;
    build)
        build
        ;;
    deploy)
        deploy
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|run|update|build|deploy}"
        echo ""
        echo "  start   - 启动服务"
        echo "  stop    - 停止服务"
        echo "  restart - 重启服务"
        echo "  status  - 查看服务状态"
        echo "  run     - 智能启动（进程存在则重启，否则启动）"
        echo "  update  - 拉取最新代码"
        echo "  build   - Maven打包"
        echo "  deploy  - 一键部署（更新代码 + 打包 + 重启）"
        exit 1
        ;;
esac
