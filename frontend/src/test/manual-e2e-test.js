/**
 * 手动端到端测试脚本
 * 验证前后端API通信是否正常
 */

import axios from 'axios';

const baseURL = 'http://localhost:8080/api/v1';

async function testE2EConnection() {
  console.log('🔍 开始端到端通信验证...\n');

  try {
    // 1. 健康检查
    console.log('1. 测试健康检查端点...');
    const healthResponse = await axios.get(`${baseURL}/health`);
    console.log(`✅ 健康检查通过: ${healthResponse.data.status}`);

    // 2. 获取所有元素
    console.log('\n2. 测试获取所有元素...');
    const elementsResponse = await axios.get(`${baseURL}/elements`);
    console.log(`✅ 获取到${elementsResponse.data.length}个元素`);
    console.log(`   元素类型: ${[...new Set(elementsResponse.data.map(e => e.eClass))].join(', ')}`);

    // 3. 按类型查询
    console.log('\n3. 测试按类型查询...');
    const reqDefResponse = await axios.get(`${baseURL}/elements?type=RequirementDefinition`);
    console.log(`✅ 获取到${reqDefResponse.data.length}个需求定义`);

    // 4. CRUD测试 - 创建
    console.log('\n4. 测试创建元素...');
    const newElement = {
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'E2E-TEST',
        declaredName: '端到端测试需求'
      }
    };
    
    const createResponse = await axios.post(`${baseURL}/elements`, newElement);
    console.log(`✅ 创建元素成功: ${createResponse.data.elementId}`);

    // 5. 验证创建的元素
    console.log('\n5. 验证创建的元素...');
    const getResponse = await axios.get(`${baseURL}/elements/${createResponse.data.elementId}`);
    console.log(`✅ 获取创建的元素成功: ${getResponse.data.declaredShortName}`);

    // 6. 更新元素
    console.log('\n6. 测试更新元素...');
    const updateData = { declaredName: '端到端测试需求-已更新' };
    const updateResponse = await axios.patch(`${baseURL}/elements/${createResponse.data.elementId}`, updateData);
    console.log(`✅ 更新元素成功: ${updateResponse.data.declaredName}`);

    // 7. 删除元素
    console.log('\n7. 测试删除元素...');
    await axios.delete(`${baseURL}/elements/${createResponse.data.elementId}`);
    console.log(`✅ 删除元素成功`);

    // 8. 验证删除
    console.log('\n8. 验证元素已删除...');
    try {
      await axios.get(`${baseURL}/elements/${createResponse.data.elementId}`);
      console.log('❌ 元素应该已被删除');
    } catch (error) {
      if (error.response && error.response.status === 404) {
        console.log('✅ 元素已被正确删除');
      } else {
        throw error;
      }
    }

    // 9. 最终状态检查
    console.log('\n9. 最终状态检查...');
    const finalResponse = await axios.get(`${baseURL}/elements`);
    const reqDefs = finalResponse.data.filter(e => e.eClass === 'RequirementDefinition');
    const reqUsages = finalResponse.data.filter(e => e.eClass === 'RequirementUsage');
    
    console.log(`✅ 当前数据状态:`);
    console.log(`   - RequirementDefinition: ${reqDefs.length}个`);
    console.log(`   - RequirementUsage: ${reqUsages.length}个`);
    console.log(`   - 总计: ${finalResponse.data.length}个元素`);

    console.log('\n🎉 所有端到端测试通过！前后端通信正常！');
    
    return {
      success: true,
      totalElements: finalResponse.data.length,
      requirementDefinitions: reqDefs.length,
      requirementUsages: reqUsages.length
    };

  } catch (error) {
    console.error('\n❌ 端到端测试失败:', error.message);
    if (error.response) {
      console.error(`   状态码: ${error.response.status}`);
      console.error(`   响应数据:`, error.response.data);
    }
    return { success: false, error: error.message };
  }
}

// 运行测试
testE2EConnection()
  .then(result => {
    if (result.success) {
      console.log('\n✅ 端到端验证完成 - 通信正常');
      process.exit(0);
    } else {
      console.log('\n❌ 端到端验证失败');
      process.exit(1);
    }
  })
  .catch(error => {
    console.error('\n❌ 测试脚本执行错误:', error);
    process.exit(1);
  });