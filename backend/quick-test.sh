# 快速测试脚本
#\!/bin/bash
echo '🧪 运行核心功能测试...'
mvn test -Dtest='*ControllerTest' --batch-mode --quiet
if [ $? -eq 0 ]; then
  echo '✅ 所有核心测试通过\!'
else
  echo '❌ 测试失败，请检查代码\!'
  exit 1
fi
