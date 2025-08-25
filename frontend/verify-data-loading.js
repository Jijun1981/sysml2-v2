// 验证数据加载的脚本
import axios from 'axios';

async function verifyDataLoading() {
  console.log('=== 验证数据加载 ===\n');
  
  try {
    // 1. 测试后端API
    console.log('1. 测试后端API...');
    const response = await axios.get('http://localhost:8080/api/v1/elements?projectId=default');
    console.log(`   ✅ 后端返回 ${response.data.length} 个元素`);
    
    // 2. 显示元素类型
    const types = [...new Set(response.data.map(el => el.eClass))];
    console.log(`   元素类型: ${types.join(', ')}`);
    
    // 3. 显示示例数据
    console.log('\n2. 示例数据:');
    const sample = response.data[0];
    if (sample) {
      console.log(`   ID: ${sample.elementId}`);
      console.log(`   类型: ${sample.eClass}`);
      console.log(`   名称: ${sample.declaredName || sample.declaredShortName || 'N/A'}`);
    }
    
    console.log('\n✅ 数据加载验证成功！');
    console.log('\n请执行以下步骤：');
    console.log('1. 打开浏览器访问 http://localhost:3000');
    console.log('2. 按 Ctrl+Shift+R 强制刷新');
    console.log('3. 查看三个视图是否显示数据');
    console.log('4. 如果没有数据，打开控制台查看错误信息');
    
  } catch (error) {
    console.error('❌ 错误:', error.message);
    if (error.code === 'ECONNREFUSED') {
      console.log('\n请确保后端服务正在运行（端口8080）');
    }
  }
}

verifyDataLoading();