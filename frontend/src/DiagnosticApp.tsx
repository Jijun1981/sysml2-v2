import React, { useEffect, useState } from 'react'

const DiagnosticApp: React.FC = () => {
  const [status, setStatus] = useState<any>({})
  
  useEffect(() => {
    // 检查各个模块
    const checkStatus = async () => {
      const results: any = {}
      
      // 1. React加载
      results.react = '✅ React加载成功'
      
      // 2. 检查Ant Design
      try {
        const antd = await import('antd')
        results.antd = antd.Button ? '✅ Ant Design加载成功' : '❌ Ant Design加载失败'
      } catch (e: any) {
        results.antd = `❌ Ant Design错误: ${e.message}`
      }
      
      // 3. 检查图标
      try {
        const icons = await import('@ant-design/icons')
        results.icons = icons.ReloadOutlined ? '✅ 图标库加载成功' : '❌ 图标库加载失败'
      } catch (e: any) {
        results.icons = `❌ 图标错误: ${e.message}`
      }
      
      // 4. 检查API
      try {
        const response = await fetch('http://localhost:8080/api/v1/projects')
        results.api = response.ok ? '✅ 后端API连接成功' : `⚠️ API响应: ${response.status}`
      } catch (e: any) {
        results.api = `❌ API连接失败: ${e.message}`
      }
      
      // 5. 检查Context
      try {
        await import('../contexts/ModelContext')
        results.context = '✅ ModelContext加载成功'
      } catch (e: any) {
        results.context = `❌ Context错误: ${e.message}`
      }
      
      // 6. 检查组件
      try {
        await import('../components/ModelViewer')
        results.modelViewer = '✅ ModelViewer加载成功'
      } catch (e: any) {
        results.modelViewer = `❌ ModelViewer错误: ${e.message}`
      }
      
      setStatus(results)
    }
    
    checkStatus()
  }, [])
  
  return (
    <div style={{ padding: '20px', fontFamily: 'monospace' }}>
      <h1>🔍 系统诊断页面</h1>
      
      <div style={{ marginTop: '20px', padding: '15px', background: '#f5f5f5', borderRadius: '5px' }}>
        <h2>模块加载状态：</h2>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          <li>{status.react || '⏳ 检查React...'}</li>
          <li>{status.antd || '⏳ 检查Ant Design...'}</li>
          <li>{status.icons || '⏳ 检查图标库...'}</li>
          <li>{status.api || '⏳ 检查后端API...'}</li>
          <li>{status.context || '⏳ 检查Context...'}</li>
          <li>{status.modelViewer || '⏳ 检查ModelViewer...'}</li>
        </ul>
      </div>
      
      <div style={{ marginTop: '20px', padding: '15px', background: '#ffe', borderRadius: '5px' }}>
        <h3>调试信息：</h3>
        <pre>{JSON.stringify({
          url: window.location.href,
          userAgent: navigator.userAgent,
          timestamp: new Date().toISOString()
        }, null, 2)}</pre>
      </div>
      
      <div style={{ marginTop: '20px' }}>
        <button 
          onClick={() => window.location.reload()}
          style={{ padding: '10px 20px', fontSize: '16px', marginRight: '10px' }}
        >
          刷新页面
        </button>
        <button 
          onClick={() => {
            console.log('Status:', status)
            alert('状态已打印到控制台')
          }}
          style={{ padding: '10px 20px', fontSize: '16px' }}
        >
          打印到控制台
        </button>
      </div>
    </div>
  )
}

export default DiagnosticApp