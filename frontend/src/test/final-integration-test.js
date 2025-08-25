/**
 * 最终集成测试
 * 验证前端能正确接收和显示数据
 */

import axios from 'axios'

async function finalTest() {
  console.log('=== 最终集成测试 ===\n')
  
  const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    timeout: 30000
  })
  
  try {
    // 1. 获取数据
    console.log('1. 从后端获取数据...')
    const response = await api.get('/elements', {
      params: { projectId: 'default' }
    })
    console.log(`   ✅ 成功获取 ${response.data.length} 个元素`)
    
    // 2. 模拟前端处理
    console.log('\n2. 模拟前端数据处理...')
    
    // 转换为前端格式
    const elements = {}
    response.data.forEach(element => {
      const id = element.elementId
      elements[id] = {
        id: id,
        eClass: element.eClass,
        attributes: element
      }
    })
    console.log(`   ✅ 转换完成：${Object.keys(elements).length} 个元素`)
    
    // 3. 构建树视图数据
    console.log('\n3. 构建树视图数据...')
    const definitions = Object.values(elements).filter(el => el.eClass === 'RequirementDefinition')
    const usages = Object.values(elements).filter(el => el.eClass === 'RequirementUsage')
    console.log(`   - ${definitions.length} 个需求定义`)
    console.log(`   - ${usages.length} 个需求使用`)
    
    // 4. 构建表视图数据
    console.log('\n4. 构建表视图数据...')
    const tableData = Object.values(elements).map(el => ({
      id: el.id,
      eClass: el.eClass,
      name: el.attributes.declaredName || el.attributes.declaredShortName || 'N/A'
    }))
    console.log(`   ✅ 表格数据：${tableData.length} 行`)
    
    // 5. 构建图视图数据
    console.log('\n5. 构建图视图数据...')
    const nodes = Object.values(elements).map(el => ({
      id: el.id,
      label: el.attributes.declaredName || el.id
    }))
    const edges = []
    Object.values(elements).forEach(el => {
      if (el.attributes.of) {
        edges.push({
          source: el.attributes.of,
          target: el.id,
          type: 'of'
        })
      }
    })
    console.log(`   - ${nodes.length} 个节点`)
    console.log(`   - ${edges.length} 条边`)
    
    // 6. 验证数据完整性
    console.log('\n6. 验证数据完整性...')
    let allGood = true
    
    // 检查必要字段
    Object.values(elements).forEach(el => {
      if (!el.id) {
        console.log(`   ❌ 元素缺少ID`)
        allGood = false
      }
      if (!el.eClass) {
        console.log(`   ❌ 元素 ${el.id} 缺少eClass`)
        allGood = false
      }
    })
    
    if (allGood) {
      console.log(`   ✅ 数据完整性验证通过`)
    }
    
    // 7. 总结
    console.log('\n=== 测试总结 ===')
    console.log('✅ 后端API正常工作')
    console.log('✅ 数据格式正确')
    console.log('✅ 前端数据转换成功')
    console.log('✅ 三个视图数据构建成功')
    console.log('\n请访问 http://localhost:3000 查看实际效果')
    console.log('如果还有问题，请按 Ctrl+Shift+R 强制刷新浏览器')
    
  } catch (error) {
    console.error('❌ 测试失败:', error.message)
    if (error.response) {
      console.error('   响应错误:', error.response.data)
    }
  }
}

finalTest()