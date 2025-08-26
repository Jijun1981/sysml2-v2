/**
 * 最简测试页面 - 验证基础功能
 */

import React, { useState } from 'react'

const SimpleTest: React.FC = () => {
  const [count, setCount] = useState(0)
  const [apiData, setApiData] = useState<any>(null)
  const [error, setError] = useState<string>('')

  const testAPI = async () => {
    try {
      setError('')
      const response = await fetch('http://localhost:8080/api/v1/elements/advanced')
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const data = await response.json()
      setApiData(data)
      console.log('API数据:', data)
    } catch (err: any) {
      setError(err.message)
      console.error('API错误:', err)
    }
  }

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h1>🎯 SysML v2 MVP - 简单测试页</h1>
      
      <div style={{ marginTop: '20px', padding: '15px', background: '#f0f0f0', borderRadius: '5px' }}>
        <h2>1. React 功能测试</h2>
        <p>计数器: {count}</p>
        <button 
          onClick={() => setCount(count + 1)}
          style={{ padding: '10px 20px', fontSize: '16px', cursor: 'pointer' }}
        >
          点击 +1
        </button>
        <p style={{ color: 'green' }}>✅ 如果点击有反应，说明React正常工作</p>
      </div>

      <div style={{ marginTop: '20px', padding: '15px', background: '#e8f4fd', borderRadius: '5px' }}>
        <h2>2. API 连接测试</h2>
        <button 
          onClick={testAPI}
          style={{ padding: '10px 20px', fontSize: '16px', cursor: 'pointer', marginBottom: '10px' }}
        >
          测试后端API
        </button>
        
        {error && (
          <div style={{ color: 'red', marginTop: '10px' }}>
            ❌ 错误: {error}
          </div>
        )}
        
        {apiData && (
          <div style={{ marginTop: '10px' }}>
            <p style={{ color: 'green' }}>✅ API连接成功!</p>
            <p>获取到 {apiData.content?.length || 0} 条数据</p>
            <details>
              <summary>查看数据详情</summary>
              <pre style={{ background: '#fff', padding: '10px', overflow: 'auto', maxHeight: '200px' }}>
                {JSON.stringify(apiData, null, 2)}
              </pre>
            </details>
          </div>
        )}
      </div>

      <div style={{ marginTop: '20px', padding: '15px', background: '#fff3e0', borderRadius: '5px' }}>
        <h2>3. 环境信息</h2>
        <ul>
          <li>前端地址: {window.location.href}</li>
          <li>后端地址: http://localhost:8080/api/v1</li>
          <li>React版本: 18.x</li>
          <li>构建工具: Vite</li>
        </ul>
      </div>

      <div style={{ marginTop: '20px', padding: '15px', background: '#e8f5e9', borderRadius: '5px' }}>
        <h2>4. 导航</h2>
        <button 
          onClick={() => window.location.href = '/'}
          style={{ padding: '10px 20px', fontSize: '16px', cursor: 'pointer', marginRight: '10px' }}
        >
          返回主应用
        </button>
        <button 
          onClick={() => window.location.href = '/test.html'}
          style={{ padding: '10px 20px', fontSize: '16px', cursor: 'pointer' }}
        >
          Ant Design测试页
        </button>
      </div>

      <div style={{ marginTop: '30px', padding: '10px', background: '#fafafa', borderRadius: '5px' }}>
        <p><strong>调试说明：</strong></p>
        <ol>
          <li>打开浏览器开发者工具（F12）</li>
          <li>查看Console标签页是否有错误</li>
          <li>查看Network标签页API请求是否正常</li>
          <li>如果有错误，请截图告诉我</li>
        </ol>
      </div>
    </div>
  )
}

export default SimpleTest