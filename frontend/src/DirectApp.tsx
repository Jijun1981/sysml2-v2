import React, { useState, useEffect } from 'react'

const DirectApp: React.FC = () => {
  const [apiData, setApiData] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>('')

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      // 测试后端API
      const response = await fetch('http://localhost:8080/api/v1/demo/dataset/small')
      if (!response.ok) {
        throw new Error(`API错误: ${response.status}`)
      }
      const data = await response.json()
      setApiData(data)
      console.log('API数据:', data)
    } catch (err: any) {
      setError(err.message)
      console.error('加载失败:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  return (
    <div style={{ padding: '20px' }}>
      <h1 style={{ color: '#1890ff' }}>SysML v2 MVP - 直连测试</h1>
      
      <div style={{ marginTop: '20px', padding: '15px', background: '#f0f2f5', borderRadius: '8px' }}>
        <h2>后端连接状态</h2>
        {loading && <p>⏳ 正在加载...</p>}
        {error && <p style={{ color: 'red' }}>❌ 错误: {error}</p>}
        {apiData && (
          <div>
            <p style={{ color: 'green' }}>✅ 后端连接成功!</p>
            <p>数据元素数量: {apiData.content?.length || 0}</p>
          </div>
        )}
      </div>

      <div style={{ marginTop: '20px', padding: '15px', background: '#fff', border: '1px solid #d9d9d9', borderRadius: '8px' }}>
        <h3>数据预览</h3>
        {apiData?.content?.slice(0, 3).map((item: any, index: number) => (
          <div key={index} style={{ marginBottom: '10px', padding: '10px', background: '#fafafa' }}>
            <strong>{item.data?.declaredName || 'Unknown'}</strong>
            <br />
            类型: {item.eClass}
            <br />
            ID: {item.data?.reqId || item.data?.elementId}
          </div>
        ))}
      </div>

      <div style={{ marginTop: '20px' }}>
        <button 
          onClick={loadData}
          style={{
            padding: '10px 20px',
            background: '#1890ff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '16px'
          }}
        >
          刷新数据
        </button>
      </div>
    </div>
  )
}

export default DirectApp