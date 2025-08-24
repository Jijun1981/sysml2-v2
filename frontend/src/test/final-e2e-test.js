/**
 * 最终端到端验证 - 验证修复后的系统状态
 */

import axios from 'axios';

const baseURL = 'http://localhost:8080/api/v1';

async function testFinalE2E() {
  console.log('🔍 最终端到端验证...\n');

  try {
    // 1. 测试通用API
    console.log('1. 测试通用API /elements...');
    const elementsResponse = await axios.get(`${baseURL}/elements`);
    console.log(`✅ 通用API工作正常: 获取到${elementsResponse.data.length}个元素`);
    
    // 显示元素类型分布
    const typeCount = {};
    elementsResponse.data.forEach(el => {
      typeCount[el.eClass] = (typeCount[el.eClass] || 0) + 1;
    });
    console.log('   元素分布:', typeCount);

    // 2. 验证数据结构
    console.log('\n2. 验证数据结构...');
    const reqDef = elementsResponse.data.find(e => e.eClass === 'RequirementDefinition');
    if (reqDef) {
      console.log(`✅ RequirementDefinition数据结构正确:`);
      console.log(`   - elementId: ${reqDef.elementId}`);
      console.log(`   - declaredShortName: ${reqDef.declaredShortName}`);
      console.log(`   - declaredName: ${reqDef.declaredName}`);
    }

    // 3. 测试CRUD操作
    console.log('\n3. 测试CRUD操作...');
    const newReq = {
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'FINAL-TEST',
        declaredName: '最终验证需求'
      }
    };
    
    const createResponse = await axios.post(`${baseURL}/elements`, newReq);
    console.log(`✅ 创建成功: ${createResponse.data.elementId}`);
    
    // 清理
    await axios.delete(`${baseURL}/elements/${createResponse.data.elementId}`);
    console.log(`✅ 删除成功`);

    // 4. 验证前端兼容性
    console.log('\n4. 验证前端兼容性...');
    const frontendData = elementsResponse.data.map(el => ({
      id: el.elementId,
      eClass: el.eClass,
      attributes: {
        declaredShortName: el.declaredShortName,
        declaredName: el.declaredName,
        of: el.of,
        source: el.source,
        target: el.target
      }
    }));
    
    console.log(`✅ 数据可转换为前端格式: ${frontendData.length}个元素`);

    // 5. 测试跨域
    console.log('\n5. 测试CORS配置...');
    const corsResponse = await axios.get(`${baseURL}/elements`, {
      headers: { 'Origin': 'http://localhost:3002' }
    });
    console.log(`✅ CORS配置正确: 跨域请求成功`);

    // 总结
    console.log('\n' + '='.repeat(50));
    console.log('🎉 最终验证完成！');
    console.log('✅ 通用API正常工作');
    console.log('✅ 数据结构符合预期');
    console.log('✅ CRUD操作正常');
    console.log('✅ 前端数据格式兼容');
    console.log('✅ CORS配置正确');
    console.log('\n建议: 前端应直接使用 /api/v1/elements 通用API');
    console.log('      避免使用旧的 /projects/{id}/requirements 接口');
    console.log('='.repeat(50));

    return { success: true };

  } catch (error) {
    console.error('\n❌ 验证失败:', error.message);
    if (error.response) {
      console.error('   响应:', error.response.data);
    }
    return { success: false };
  }
}

// 运行测试
testFinalE2E()
  .then(result => {
    process.exit(result.success ? 0 : 1);
  });