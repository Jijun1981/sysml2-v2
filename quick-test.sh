#!/bin/bash

# SysML v2 快速回归测试 - 单人开发专用
# 用法: ./quick-test.sh

echo "🔍 SysML v2 快速回归测试开始..."

# 检查服务
echo "1. 检查后端服务..."
if ! curl -s http://localhost:8080/actuator/health >/dev/null; then
    echo "❌ 后端服务未运行，请先启动："
    echo "   cd backend && mvn spring-boot:run"
    exit 1
else
    echo "✅ 后端服务运行正常"
fi

# 测试基础CRUD
echo "2. 测试基础CRUD功能..."
TEST_ID="QUICK-$(date +%s)"

# 创建需求
echo "   创建需求..."
CREATE_RESULT=$(curl -s -X POST "http://localhost:8080/api/v1/requirements" \
    -H "Content-Type: application/json" \
    -d "{\"elementId\":\"$TEST_ID\",\"reqId\":\"$TEST_ID\",\"declaredShortName\":\"快速测试\",\"declaredName\":\"快速回归测试需求描述\"}")

if echo "$CREATE_RESULT" | grep -q "$TEST_ID"; then
    echo "✅ 创建功能正常"
    
    # 读取验证
    echo "   验证读取..."
    if curl -s http://localhost:8080/api/v1/requirements | grep -q "$TEST_ID"; then
        echo "✅ 读取功能正常"
        
        # 删除验证
        echo "   删除需求..."
        curl -s -X DELETE "http://localhost:8080/api/v1/requirements/$TEST_ID" >/dev/null
        
        if ! curl -s http://localhost:8080/api/v1/requirements | grep -q "$TEST_ID"; then
            echo "✅ 删除功能正常"
        else
            echo "❌ 删除功能异常 - 这是关键问题！"
            exit 1
        fi
    else
        echo "❌ 读取功能异常"
        exit 1
    fi
else
    echo "❌ 创建功能异常"
    echo "响应内容: $CREATE_RESULT"
    exit 1
fi

# 测试API健康状况
echo "3. 测试API健康状况..."
HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"UP"' || echo "")
if [ -n "$HEALTH_STATUS" ]; then
    echo "✅ API健康检查通过"
else
    echo "❌ API健康检查失败"
    exit 1
fi

echo ""
echo "🎉 快速回归测试完成 - 所有核心功能正常！"
echo ""
echo "💡 提示:"
echo "   - 如果需要完整测试，运行: ./scripts/regression-suite.sh core"
echo "   - 删除持久化是关键功能，如果失败请立即修复"
echo "   - 详细指南请查看: REGRESSION-TEST-GUIDE.md"