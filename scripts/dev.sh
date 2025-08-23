#!/bin/bash

# SysML MVP 开发环境启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  SysML v2 MVP 开发环境启动脚本"
echo "============================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查Java环境
check_java() {
    echo -n "检查Java环境... "
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "$JAVA_VERSION" -ge 17 ]]; then
            echo -e "${GREEN}✓${NC} Java $JAVA_VERSION"
        else
            echo -e "${RED}✗${NC} 需要Java 17或更高版本"
            exit 1
        fi
    else
        echo -e "${RED}✗${NC} 未找到Java"
        exit 1
    fi
}

# 检查Maven环境
check_maven() {
    echo -n "检查Maven环境... "
    if command -v mvn &> /dev/null; then
        echo -e "${GREEN}✓${NC} $(mvn -version | head -n 1)"
    else
        echo -e "${RED}✗${NC} 未找到Maven"
        exit 1
    fi
}

# 检查Node.js环境
check_node() {
    echo -n "检查Node.js环境... "
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
        if [[ "$NODE_VERSION" -ge 16 ]]; then
            echo -e "${GREEN}✓${NC} Node.js $(node -v)"
        else
            echo -e "${RED}✗${NC} 需要Node.js 16或更高版本"
            exit 1
        fi
    else
        echo -e "${RED}✗${NC} 未找到Node.js"
        exit 1
    fi
}

# 创建数据目录
setup_data_dirs() {
    echo "创建数据目录..."
    mkdir -p "$PROJECT_ROOT/data/projects"
    mkdir -p "$PROJECT_ROOT/data/backups"
    mkdir -p "$PROJECT_ROOT/data/demo"
    mkdir -p "$PROJECT_ROOT/data/logs"
    echo -e "${GREEN}✓${NC} 数据目录已创建"
}

# 安装后端依赖
install_backend() {
    echo -e "\n${YELLOW}安装后端依赖...${NC}"
    cd "$PROJECT_ROOT/backend"
    mvn clean install -DskipTests
    echo -e "${GREEN}✓${NC} 后端依赖安装完成"
}

# 安装前端依赖
install_frontend() {
    echo -e "\n${YELLOW}安装前端依赖...${NC}"
    cd "$PROJECT_ROOT/frontend"
    npm install
    echo -e "${GREEN}✓${NC} 前端依赖安装完成"
}

# 启动后端服务
start_backend() {
    echo -e "\n${YELLOW}启动后端服务...${NC}"
    cd "$PROJECT_ROOT/backend"
    mvn spring-boot:run &
    BACKEND_PID=$!
    echo "后端服务PID: $BACKEND_PID"
    
    # 等待后端启动
    echo -n "等待后端服务启动"
    for i in {1..30}; do
        if curl -s http://localhost:8080/api/v1/health > /dev/null; then
            echo -e "\n${GREEN}✓${NC} 后端服务已启动"
            break
        fi
        echo -n "."
        sleep 1
    done
}

# 启动前端服务
start_frontend() {
    echo -e "\n${YELLOW}启动前端服务...${NC}"
    cd "$PROJECT_ROOT/frontend"
    npm run dev &
    FRONTEND_PID=$!
    echo "前端服务PID: $FRONTEND_PID"
    echo -e "${GREEN}✓${NC} 前端服务已启动"
}

# 清理函数
cleanup() {
    echo -e "\n${YELLOW}停止服务...${NC}"
    if [[ -n "$BACKEND_PID" ]]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [[ -n "$FRONTEND_PID" ]]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    echo -e "${GREEN}✓${NC} 服务已停止"
}

# 设置信号处理
trap cleanup EXIT INT TERM

# 主流程
main() {
    echo -e "\n${YELLOW}环境检查${NC}"
    check_java
    check_maven
    check_node
    
    echo -e "\n${YELLOW}初始化${NC}"
    setup_data_dirs
    
    # 根据参数决定是否安装依赖
    if [[ "$1" == "--install" ]]; then
        install_backend
        install_frontend
    fi
    
    # 启动服务
    start_backend
    sleep 3
    start_frontend
    
    echo -e "\n============================================"
    echo -e "${GREEN}开发环境启动成功！${NC}"
    echo -e "后端服务: http://localhost:8080/api/v1"
    echo -e "前端服务: http://localhost:3000"
    echo -e "API文档: http://localhost:8080/api/v1/swagger-ui.html"
    echo -e "健康检查: http://localhost:8080/api/v1/health"
    echo -e "\n按 Ctrl+C 停止服务"
    echo -e "============================================\n"
    
    # 保持脚本运行
    while true; do
        sleep 1
    done
}

# 执行主流程
main "$@"