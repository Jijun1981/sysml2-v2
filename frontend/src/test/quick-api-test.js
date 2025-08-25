/**
 * 快速API测试脚本
 * 测试前端universalApi是否正确发送请求
 */

import axios from 'axios'

async function testAPI() {
  console.log('=== 测试前后端通信 ===\n')
  
  // 1. 测试后端直接调用
  console.log('1. 测试后端API (带projectId)...')
  try {
    const response = await axios.get('http://localhost:8080/api/v1/elements?projectId=default')
    console.log(`   ✅ 成功：返回 ${response.data.length} 个元素`)
  } catch (error) {
    console.log(`   ❌ 失败：${error.response?.data?.error || error.message}`)
  }
  
  // 2. 测试后端不带projectId
  console.log('\n2. 测试后端API (不带projectId)...')
  try {
    const response = await axios.get('http://localhost:8080/api/v1/elements')
    console.log(`   ✅ 成功：返回 ${response.data.length} 个元素`)
  } catch (error) {
    console.log(`   ❌ 失败：${error.response?.data?.error || error.message}`)
  }
  
  // 3. 测试前端API格式
  console.log('\n3. 模拟前端universalApi...')
  const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    timeout: 30000
  })
  
  try {
    // 模拟 queryAllElements
    const projectId = 'default'
    const response = await api.get('/elements', {
      params: { projectId }
    })
    console.log(`   ✅ 成功：返回 ${response.data.length} 个元素`)
    
    // 验证数据格式
    if (response.data.length > 0) {
      const first = response.data[0]
      console.log('\n   第一个元素：')
      console.log(`   - elementId: ${first.elementId}`)
      console.log(`   - eClass: ${first.eClass}`)
      console.log(`   - declaredName: ${first.declaredName}`)
    }
  } catch (error) {
    console.log(`   ❌ 失败：${error.response?.data?.error || error.message}`)
  }
  
  // 4. 测试数据转换
  console.log('\n4. 测试数据转换（模拟ModelContext）...')
  try {
    const response = await axios.get('http://localhost:8080/api/v1/elements?projectId=default')
    
    // 模拟 ModelContext 的数据处理
    const elementsToAdd = (response.data || []).reduce((acc, element) => {
      const id = element.elementId || element.id
      acc[id] = {
        ...element,
        id: id,
        attributes: {
          declaredName: element.declaredName,
          declaredShortName: element.declaredShortName,
          of: element.of,
          source: element.source,
          target: element.target,
          status: element.status || 'active',
          ...element
        }
      }
      return acc
    }, {})
    
    console.log(`   ✅ 转换成功：${Object.keys(elementsToAdd).length} 个元素`)
    
    // 显示一个示例
    const firstKey = Object.keys(elementsToAdd)[0]
    if (firstKey) {
      const el = elementsToAdd[firstKey]
      console.log('\n   转换后的第一个元素：')
      console.log(`   - id: ${el.id}`)
      console.log(`   - eClass: ${el.eClass}`)
      console.log(`   - attributes.declaredName: ${el.attributes.declaredName}`)
    }
  } catch (error) {
    console.log(`   ❌ 失败：${error.message}`)
  }
  
  console.log('\n=== 测试完成 ===')
}

testAPI()