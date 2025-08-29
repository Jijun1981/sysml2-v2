#!/bin/bash
# SysML MVP 回归测试套件

echo "================================================"
echo "🧪 SysML MVP 回归测试套件"
echo "================================================"
echo ""

# 选择测试级别
echo "请选择测试级别："
echo "1) 快速测试 - Controller层（2分钟）"
echo "2) 标准测试 - 所有单元测试（5分钟）"
echo "3) 完整测试 - 清理编译+全测试（10分钟）"
echo -n "选择 (1-3，默认2): "
read -t 10 choice
choice=${choice:-2}

case $choice in
    1)
        echo "🚀 执行快速测试..."
        mvn test -Dtest="*ControllerTest" --batch-mode --quiet
        ;;
    2)
        echo "📊 执行标准测试..."
        mvn test --batch-mode
        ;;
    3)
        echo "🔧 执行完整测试（包含清理和编译）..."
        mvn clean test
        ;;
    *)
        echo "❌ 无效选择，执行标准测试..."
        mvn test --batch-mode
        ;;
esac

# 检查测试结果
if [ $? -eq 0 ]; then
    echo ""
    echo "================================================"
    echo "✅ 所有测试通过！代码质量OK，可以安全提交。"
    echo "================================================"
    
    # 显示测试统计
    echo ""
    echo "📈 测试统计："
    grep -h "Tests run:" target/surefire-reports/*.txt 2>/dev/null | tail -5
    
else
    echo ""
    echo "================================================"
    echo "❌ 测试失败！请修复问题后再提交。"
    echo "================================================"
    
    # 显示失败的测试
    echo ""
    echo "失败的测试："
    grep -l "FAILURE" target/surefire-reports/*.txt 2>/dev/null | xargs basename -s .txt
    
    exit 1
fi