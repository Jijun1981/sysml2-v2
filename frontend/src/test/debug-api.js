/**
 * 调试API响应
 */

import { queryAllElements } from '../services/universalApi.js'

async function debugAPI() {
  console.log('=== 调试前端API响应 ===\n')
  
  try {
    console.log('1. 调用 queryAllElements()...')
    const response = await queryAllElements()
    
    console.log('2. 响应结构:')
    console.log('   - response:', response)
    console.log('   - response.data:', response.data)
    console.log('   - response.data长度:', response.data ? response.data.length : 'undefined')
    
    if (response.data && response.data.length > 0) {
      console.log('\n3. 第一个元素:')
      console.log(JSON.stringify(response.data[0], null, 2))
    }
    
    console.log('\n4. 完整响应（前500字符）:')
    console.log(JSON.stringify(response).substring(0, 500))
    
  } catch (error) {
    console.error('错误:', error.message)
    console.error('详细:', error)
  }
}

debugAPI()