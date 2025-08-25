/**
 * 测试ModelContext是否正确加载数据
 */
import React from 'react'
import { ModelProvider, useModelContext } from '../contexts/ModelContext'

const DebugDisplay: React.FC = () => {
  const { elements, loading, error } = useModelContext()
  
  console.log('DebugDisplay - elements:', elements)
  console.log('DebugDisplay - loading:', loading)
  console.log('DebugDisplay - error:', error)
  
  return (
    <div style={{ padding: '20px' }}>
      <h1>ModelContext 调试信息</h1>
      
      <div>
        <strong>Loading:</strong> {loading ? '加载中...' : '已完成'}
      </div>
      
      <div>
        <strong>Error:</strong> {error ? error.message : '无错误'}
      </div>
      
      <div>
        <strong>Elements 数量:</strong> {Object.keys(elements).length}
      </div>
      
      <div>
        <strong>Elements 详情:</strong>
        <pre>{JSON.stringify(elements, null, 2)}</pre>
      </div>
    </div>
  )
}

const DebugApp: React.FC = () => {
  return (
    <ModelProvider>
      <DebugDisplay />
    </ModelProvider>
  )
}

export default DebugApp