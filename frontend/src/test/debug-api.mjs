/**
 * 调试API响应
 */

import axios from 'axios'

async function debugAPI() {
  console.log('=== 调试前端API响应 ===\n')
  
  try {
    // 直接调用后端
    console.log('1. 直接调用后端 /api/v1/elements...')
    const directResponse = await axios.get('http://localhost:8080/api/v1/elements?projectId=default')
    console.log('   - 后端返回元素数量:', directResponse.data.length)
    
    // 模拟前端的queryAllElements
    console.log('\n2. 模拟前端queryAllElements...')
    const api = axios.create({
      baseURL: 'http://localhost:8080/api/v1',
      timeout: 30000
    })
    
    const projectId = 'default'
    const { data } = await api.get('/elements', {
      params: { projectId }
    })
    
    // 模拟前端的包装
    const wrappedResponse = {
      data: data || [],
      meta: { page: 0, size: 100, total: data?.length || 0 },
      timestamp: new Date().toISOString()
    }
    
    console.log('   - wrappedResponse.data长度:', wrappedResponse.data.length)
    console.log('   - wrappedResponse结构:', Object.keys(wrappedResponse))
    
    // 模拟ModelContext的处理
    console.log('\n3. 模拟ModelContext处理...')
    const elementsToAdd = (wrappedResponse.data || []).reduce((acc, element) => {
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
    
    console.log('   - 处理后的元素数量:', Object.keys(elementsToAdd).length)
    console.log('   - 第一个元素ID:', Object.keys(elementsToAdd)[0])
    
    if (Object.keys(elementsToAdd).length > 0) {
      const firstKey = Object.keys(elementsToAdd)[0]
      console.log('   - 第一个元素内容:')
      console.log(JSON.stringify(elementsToAdd[firstKey], null, 2))
    }
    
  } catch (error) {
    console.error('错误:', error.message)
    if (error.response) {
      console.error('响应错误:', error.response.data)
    }
  }
}

debugAPI()